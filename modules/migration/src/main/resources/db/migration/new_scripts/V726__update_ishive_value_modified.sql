UPDATE products as p SET ishiv = CASE WHEN(tmp.code = 'MMIA' or tmp.parentid=1) THEN true ELSE false END
FROM (select pt.id,pg.code,pg.parentid from products pt
  left join program_products pp on pt.id = pp.productid
  left join programs pg on pp.programid = pg.id ) as tmp WHERE p.id = tmp.id;