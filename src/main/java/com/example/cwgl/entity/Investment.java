package com.example.cwgl.entity;

import lombok.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Investment {

  private Integer id;
  private Integer userid;
  private Integer houseid;
  private String realname;
  private String name;
  private String info;
  private String risk;
  private BigDecimal earnings;


}
