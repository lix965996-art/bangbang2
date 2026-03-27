package com.farmland.intel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 农作物在线销售实体
 */
@Getter
@Setter
@TableName("online_sale")
@ApiModel(value = "OnlineSale对象", description = "农作物在线销售表")
public class OnlineSale implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("关联库存ID")
    private Integer inventoryId;

    @ApiModelProperty("商品名称")
    private String produce;

    @ApiModelProperty("所属仓库")
    private String warehouse;

    @ApiModelProperty("出售数量")
    private Integer quantity;

    @ApiModelProperty("单价(元)")
    private BigDecimal price;

    @ApiModelProperty("总价(元)")
    private BigDecimal totalPrice;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("销售员")
    private String seller;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty("备注")
    private String remark;
}
