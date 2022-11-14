package com.example.cwgl.entity;

import lombok.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Work {

  private Integer id;
  private Integer userid;
  private Integer houseid;
  private String realname;
  private Integer workTime;
  private String workName;
  private String workType;
  private BigDecimal earnings;
  private String manner;

}
