package org.openlmis.rnr.repository.mapper;

import org.apache.ibatis.annotations.*;
import org.openlmis.rnr.domain.PatientQuantificationLineItem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientQuantificationLineItemMapper {

    @Insert({"INSERT INTO patient_quantification_line_items(category, total, rnrId, modifiedBy, createdBy, tableName) values " +
            "(#{category}, #{total}, #{rnrId}, #{modifiedBy}, #{createdBy}, #{tableName})"})
    @Options(useGeneratedKeys = true)
    void insert(PatientQuantificationLineItem patientQuantificationLineItem);

    @Select("SELECT * FROM patient_quantification_line_items WHERE rnrId = #{rnrId}")
    @Results(value = {
            @Result(property = "id", column = "id"),
            @Result(property = "category", column = "category"),
            @Result(property = "total", column = "total"),
            @Result(property = "tableName", column = "tablename")
    })
    List<PatientQuantificationLineItem> getPatientQuantificationLineItemsByRnrId(Long rnrId);
}
