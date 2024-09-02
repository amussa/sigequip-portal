CREATE OR REPLACE FUNCTION delete_product_and_dependencies(v_product_code VARCHAR)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    v_product_id INT;
    v_lot_id INT;
    r RECORD;
    v_error_message TEXT;
    v_id_to_delete INT;
BEGIN
    -- Buscar o `product_id` com base no `v_product_code`
    SELECT id INTO v_product_id FROM products WHERE code = v_product_code;
    
    IF v_product_id IS NULL THEN
        RAISE NOTICE 'Produto com o código % não encontrado.', v_product_code;
        RETURN;
    END IF;

    -- 1. Apagar registos nas tabelas que dependem de `lots`
    FOR r IN 
        SELECT id FROM lots WHERE productid = v_product_id
    LOOP
        v_lot_id := r.id;

        -- 1.1 Apagar registos na tabela `stock_card_entry_lot_items_key_values` usando `lotid`
        FOR r IN 
            SELECT id FROM stock_card_entry_lot_items WHERE lotid = v_lot_id
        LOOP
            BEGIN
                EXECUTE 'DELETE FROM stock_card_entry_lot_items_key_values WHERE stockcardentrylotitemid = ' || r.id || ';';
                RAISE NOTICE 'Deleted from stock_card_entry_lot_items_key_values where stockcardentrylotitemid = %', r.id;
            EXCEPTION
                WHEN OTHERS THEN
                    v_error_message := SQLERRM;
                    IF v_error_message ~ 'Key \(id\)=\((\d+)\)' THEN
                        v_id_to_delete := (regexp_matches(v_error_message, 'Key \(id\)=\((\d+)\)'))[1]::INT;
                        EXECUTE 'DELETE FROM stock_card_entry_lot_items_key_values WHERE stockcardentrylotitemid = ' || v_id_to_delete || ';';
                        RAISE NOTICE 'Deleted problematic entry with id %', v_id_to_delete;
                    END IF;
            END;
        END LOOP;

        -- 1.2 Apagar registos na tabela `stock_card_entry_lot_items`
        FOR r IN 
            SELECT id FROM stock_card_entry_lot_items WHERE lotid = v_lot_id
        LOOP
            BEGIN
                EXECUTE 'DELETE FROM stock_card_entry_lot_items WHERE id = ' || r.id || ';';
                RAISE NOTICE 'Deleted from stock_card_entry_lot_items where id = %', r.id;
            EXCEPTION
                WHEN OTHERS THEN
                    v_error_message := SQLERRM;
                    IF v_error_message ~ 'Key \(id\)=\((\d+)\)' THEN
                        v_id_to_delete := (regexp_matches(v_error_message, 'Key \(id\)=\((\d+)\)'))[1]::INT;
                        EXECUTE 'DELETE FROM stock_card_entry_lot_items WHERE id = ' || v_id_to_delete || ';';
                        RAISE NOTICE 'Deleted problematic entry with id %', v_id_to_delete;
                    END IF;
            END;
        END LOOP;

        -- 1.3 Apagar registos em outras tabelas que referenciam `lotid`
        EXECUTE 'DELETE FROM stock_movement_lots WHERE lotid = ' || v_lot_id || ';';
        EXECUTE 'DELETE FROM stock_movement_line_items WHERE lotid = ' || v_lot_id || ';';
        EXECUTE 'DELETE FROM lots_on_hand WHERE lotid = ' || v_lot_id || ';';
        EXECUTE 'DELETE FROM lot_conflicts WHERE lotid = ' || v_lot_id || ';';
        RAISE NOTICE 'Deleted from related tables where lotid = %', v_lot_id;

        -- 1.4 Apagar finalmente o registo na tabela `lots`
        EXECUTE 'DELETE FROM lots WHERE id = ' || v_lot_id || ';';
        RAISE NOTICE 'Deleted from lots where id = %', v_lot_id;
    END LOOP;

    -- 2. Apagar registos na tabela `cmm_entries` que referenciam `productcode`
    EXECUTE 'DELETE FROM cmm_entries WHERE productcode = ''' || v_product_code || ''';';
    RAISE NOTICE 'Deleted from cmm_entries where productcode = %', v_product_code;

    -- 3. Apagar registos na tabela `cmm_entries_bak` que referenciam `productcode`
    EXECUTE 'DELETE FROM cmm_entries_bak WHERE productcode = ''' || v_product_code || ''';';
    RAISE NOTICE 'Deleted from cmm_entries_bak where productcode = %', v_product_code;

    -- 4. Apagar registos na tabela `stock_card_entry_key_values` que referenciam `stockcardentryid`
    FOR r IN 
        SELECT id FROM stock_card_entries WHERE stockcardid IN (SELECT id FROM stock_cards WHERE productid = v_product_id)
    LOOP
        BEGIN
            EXECUTE 'DELETE FROM stock_card_entry_key_values WHERE stockcardentryid = ' || r.id || ';';
            RAISE NOTICE 'Deleted from stock_card_entry_key_values where stockcardentryid = %', r.id;
        EXCEPTION
            WHEN OTHERS THEN
                v_error_message := SQLERRM;
                IF v_error_message ~ 'Key \(id\)=\((\d+)\)' THEN
                    v_id_to_delete := (regexp_matches(v_error_message, 'Key \(id\)=\((\d+)\)'))[1]::INT;
                    EXECUTE 'DELETE FROM stock_card_entry_key_values WHERE stockcardentryid = ' || v_id_to_delete || ';';
                    RAISE NOTICE 'Deleted problematic entry with id %', v_id_to_delete;
                END IF;
        END;
    END LOOP;

    -- 5. Apagar registos na tabela `stock_card_entries` que referenciam `stock_card_id`
    FOR r IN 
        SELECT id FROM stock_cards WHERE productid = v_product_id
    LOOP
        BEGIN
            EXECUTE 'DELETE FROM stock_card_entries WHERE stockcardid = ' || r.id || ';';
            RAISE NOTICE 'Deleted from stock_card_entries where stockcardid = %', r.id;
        EXCEPTION
            WHEN OTHERS THEN
                v_error_message := SQLERRM;
                IF v_error_message ~ 'Key \(id\)=\((\d+)\)' THEN
                    v_id_to_delete := (regexp_matches(v_error_message, 'Key \(id\)=\((\d+)\)'))[1]::INT;
                    EXECUTE 'DELETE FROM stock_card_entries WHERE id = ' || v_id_to_delete || ';';
                    RAISE NOTICE 'Deleted problematic entry with id %', v_id_to_delete;
                END IF;
        END;
    END LOOP;

    -- 6. Apagar registos na tabela `stock_cards` que referenciam `productid`
    EXECUTE 'DELETE FROM stock_cards WHERE productid = ' || v_product_id || ';';
    RAISE NOTICE 'Deleted from stock_cards where productid = %', v_product_id;

    -- 7. Apagar registos na tabela `facility_approved_products` que referenciam `programproductid`
    FOR r IN 
        SELECT id FROM program_products WHERE productid = v_product_id
    LOOP
        BEGIN
            EXECUTE 'DELETE FROM facility_approved_products WHERE programproductid = ' || r.id || ';';
            RAISE NOTICE 'Deleted from facility_approved_products where programproductid = %', r.id;
        EXCEPTION
            WHEN OTHERS THEN
                v_error_message := SQLERRM;
                IF v_error_message ~ 'Key \(id\)=\((\d+)\)' THEN
                    v_id_to_delete := (regexp_matches(v_error_message, 'Key \(id\)=\((\d+)\)'))[1]::INT;
                    EXECUTE 'DELETE FROM facility_approved_products WHERE id = ' || v_id_to_delete || ';';
                    RAISE NOTICE 'Deleted problematic entry with id %', v_id_to_delete;
                END IF;
        END;
    END LOOP;

    -- 8. Apagar registos na tabela `program_products` que referenciam `productid`
    EXECUTE 'DELETE FROM program_products WHERE productid = ' || v_product_id || ';';
    RAISE NOTICE 'Deleted from program_products where productid = %', v_product_id;

    -- 9. Apagar registos na tabela `requisition_line_items` que referenciam `productcode`
    EXECUTE 'DELETE FROM requisition_line_items WHERE productcode = ''' || v_product_code || ''';';
    RAISE NOTICE 'Deleted from requisition_line_items where productcode = %', v_product_code;

    -- 10. Apagar finalmente o registo na tabela `products`
    DELETE FROM products WHERE id = v_product_id AND code = v_product_code;
    RAISE NOTICE 'Produto com ID = %, Code = % apagado com sucesso.', v_product_id, v_product_code;
END $$;

