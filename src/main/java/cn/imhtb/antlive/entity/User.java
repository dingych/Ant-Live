package cn.imhtb.antlive.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author PinTeh
 */
@Data
@TableName("tb_user")
public class User {

    private Integer id;

    @TableField(exist = false)
    private String account;

    @JsonIgnore
    private String password;

    private String email;

    private String mobile;

    private String avatar;

    private String nickName;

    private String sex;

    private Integer role;

    private int disabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
