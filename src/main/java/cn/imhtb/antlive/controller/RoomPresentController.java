package cn.imhtb.antlive.controller;

import cn.imhtb.antlive.common.ApiResponse;
import cn.imhtb.antlive.entity.database.RoomPresent;
import cn.imhtb.antlive.service.IRoomPresentService;
import cn.imhtb.antlive.utils.JwtUtils;
import cn.imhtb.antlive.vo.response.RoomPresentResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author PinTeh
 * @date 2020/4/8
 */
@RestController
@RequestMapping("/room/present")
public class RoomPresentController {

    //private String column = "id,from_id,to_id,room_id,present_id,number,unit_price,total_price,create_time,update_time,present_name";

    private final IRoomPresentService roomPresentService;

    public RoomPresentController(IRoomPresentService roomPresentService) {
        this.roomPresentService = roomPresentService;
    }

    @GetMapping("/list")
    public ApiResponse list(HttpServletRequest request
            , @RequestParam(required = false, defaultValue = "10") Integer limit
            , @RequestParam(required = false, defaultValue = "1") Integer page
            , @RequestParam(required = false) String dateRange) {
        Integer uid = JwtUtils.getId(request);
        String maxTime = "", minTime = "";
        boolean condition = !StringUtils.isEmpty(dateRange) && !"null".equals(dateRange);
        if (condition) {
            maxTime = dateRange.split(",")[1];
            minTime = dateRange.split(",")[0];
        }
        QueryWrapper<RoomPresent> wrapper = new QueryWrapper<RoomPresent>()
                .eq("to_id", uid)
                .le(condition,"create_time",maxTime)
                .ge(condition,"create_time",minTime)
                .orderByDesc("id");
        Page<RoomPresent> roomPresentPage = roomPresentService.page(new Page<>(page, limit), wrapper);
        return ApiResponse.ofSuccess(roomPresentPage);
    }

    private List<RoomPresentResponse> packagePresentName(List<RoomPresent> list) {
        return null;
    }
}
