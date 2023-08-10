package com.cdac.enrollmentstation.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContractorDetail {
    String contractorId;
    String serialNo;
    String contactId;
    String contractorName;
    int cardReaderHandle;

}
