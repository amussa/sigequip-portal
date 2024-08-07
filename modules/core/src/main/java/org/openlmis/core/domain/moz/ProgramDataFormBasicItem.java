package org.openlmis.core.domain.moz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openlmis.core.domain.BaseModel;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramDataFormBasicItem extends BaseModel {

    private String productCode;
    private Integer beginningBalance;
    private Integer quantityReceived;
    private Integer quantityDispensed;
    private Integer totalLossesAndAdjustments;
    private Integer stockInHand;
    private String expirationDate;
    private ProgramDataForm programDataForm;
}
