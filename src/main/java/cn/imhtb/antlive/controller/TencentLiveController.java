package cn.imhtb.antlive.controller;

import cn.imhtb.antlive.common.ApiResponse;
import cn.imhtb.antlive.common.Constants;
import cn.imhtb.antlive.entity.LiveInfo;
import cn.imhtb.antlive.entity.Room;
import cn.imhtb.antlive.entity.database.LiveDetect;
import cn.imhtb.antlive.entity.tencent.ShotRuleResponse;
import cn.imhtb.antlive.entity.tencent.StreamResponse;
import cn.imhtb.antlive.service.ILiveDetectService;
import cn.imhtb.antlive.service.ILiveInfoService;
import cn.imhtb.antlive.service.IRoomService;
import cn.imhtb.antlive.service.ITencentLiveService;
import cn.imhtb.antlive.utils.CommonUtils;
import cn.imhtb.antlive.utils.JwtUtils;
import cn.imhtb.antlive.service.impl.TencentLiveServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * @author PinTeh
 * @date 2020/4/7
 */
@Slf4j
@RestController
@RequestMapping("/tencent/live")
public class TencentLiveController {

    @Value("${tencent.live.domain}")
    private String domain;

    @Value("${tencent.live.appName}")
    private String appName;

    private final IRoomService roomService;

    private final ILiveInfoService liveInfoService;

    private final ILiveDetectService liveDetectService;

    private final ITencentLiveService tencentLiveService;

    private final ModelMapper modelMapper;

    public TencentLiveController(IRoomService roomService, ILiveInfoService liveInfoService, ModelMapper modelMapper, ILiveDetectService liveDetectService, ITencentLiveService tencentLiveService) {
        this.roomService = roomService;
        this.liveInfoService = liveInfoService;
        this.modelMapper = modelMapper;
        this.liveDetectService = liveDetectService;
        this.tencentLiveService = tencentLiveService;
    }

    @GetMapping("/open")
    public ApiResponse open(HttpServletRequest request){
        // get token
        String token = request.getHeader(JwtUtils.getHeaderKey());
        Integer uid = JwtUtils.getId(token);
        // get room by uid
        Room r = roomService.getOne(new QueryWrapper<Room>().eq("user_id",uid).last("limit 0,1"));
        // generator push url
        long txTime = LocalDateTime.now().plusHours(12L).toInstant(ZoneOffset.of("+8")).toEpochMilli();
        String safeUrl = TencentLiveServiceImpl.getSafeUrl("", String.valueOf(r.getId()), txTime/1000);
        Map<String,String> ret = new HashMap<>(2);
        ret.put("pushUrl","rtmp://" + domain + "/" + appName + "/");
        ret.put("secret",+ r.getId() + "?" + safeUrl);
        return ApiResponse.ofSuccess(ret);
    }

    /*
    StreamResponse(app=live.imhtb.cn,
                   appid=1253825991,
                   appname=live,
                   channelId=null,
                   errcode=0,
                   errmsg=ok,
                   eventTime=0,
                   eventType=0,
                   node=125.94.63.175,
                   sequence=7001420704626954503,
                   streamId=null,
                   streamParam=null,
                   userIp=null,
                   sign=613bf306b7fa5cf7e295babe77868e2a,
                   t=1586273219)
           */

    /**
     * stream push recall
     */
    @RequestMapping("/start")
    public void start(@RequestBody StreamResponse response){
        log.info("tencent live recall start ...");
        log.info("response:" + response);
        String sign = DigestUtils.md5DigestAsHex(("AntLive" + response.getT()).getBytes());
        log.info("sign:" + sign + " ret:" + response.getSign().equals(sign));
        if (!response.getSign().equals(sign)){
            log.warn("illegal request! stream_id:" + response.getStream_id());
            return;
        }

        Room room = roomService.getById(Integer.valueOf(response.getStream_id()));
        if (room == null || room.getStatus() != Constants.LiveStatus.STOP.getCode()){
            log.warn("live start fail : because room = null or room's status not equals stop code");
            return;
        }

        room.setSecret(CommonUtils.getRandomString());
        room.setStatus(Constants.LiveStatus.LIVING.getCode());
        roomService.updateById(room);

        // record live duration
        LiveInfo liveInfo = new LiveInfo();
        liveInfo.setUserId(room.getUserId());
        liveInfo.setRoomId(room.getId());
        liveInfo.setStatus(Constants.LiveInfoStatus.NO.getCode());
        liveInfo.setStartTime(LocalDateTime.now());
        liveInfoService.save(liveInfo);

    }

    /**
     * stream end recall
     */
    @RequestMapping("/end")
    public ApiResponse end(@RequestBody StreamResponse response){
        log.info("tencent live recall end ...");
        String sign = DigestUtils.md5DigestAsHex(("AntLive" + response.getT()).getBytes());
        log.info("sign:" + sign + " ret:" + response.getSign().equals(sign));
        if (!response.getSign().equals(sign)){
            return ApiResponse.ofError("illegal request! stream_id:" + response.getStream_id());
        }

        Integer rid = Integer.valueOf(response.getStream_id());
        // update room status
        Room room = new Room();
        room.setId(rid);
        room.setStatus(Constants.LiveStatus.STOP.getCode());
        roomService.updateById(room);

        // update live info, add end time
        LiveInfo liveInfo = liveInfoService.getOne(new QueryWrapper<LiveInfo>().eq("room_id", rid).orderByDesc("id").last("limit 0,1"));
        if (liveInfo.getEndTime()!=null){
            return ApiResponse.ofSuccess();
        }
        // 不能直接更新liveInfo
        LiveInfo updateInfo = new LiveInfo();
        updateInfo.setId(liveInfo.getId());
        updateInfo.setEndTime(LocalDateTime.now());
        // 0-living 1-finished
        updateInfo.setStatus(Constants.LiveInfoStatus.YES.getCode());
        liveInfoService.updateById(updateInfo);
        return ApiResponse.ofSuccess();
    }

    /**
     * screenshot recall
     */
    @RequestMapping("/screenshot")
    public void screenshot(){

    }

    /**
     * appraise salacity recall
     * type 图片类型， 0 ：正常图片， 1 ：色情图片， 2 ：性感图片， 3 ：涉政图片， 4 ：违法图片， 5 ：涉恐图片 ，6 - 9 ：其他其它图片
     */
    @RequestMapping("/appraise")
    public void appraise(@RequestBody ShotRuleResponse response) {
        log.info("----------" + response.toString());
        int confidence = response.getConfidence();
        Integer rid = Integer.valueOf(response.getStreamId());

        LiveDetect liveDetect = modelMapper.map(response, LiveDetect.class);
        liveDetect.setRoomId(rid);
        StringBuilder builder = new StringBuilder();
        for (Integer integer : response.getType()) {
            builder.append(integer).append(",");
        }
        String type = builder.toString();
        liveDetect.setType(type.substring(0,type.length()-1));
        liveDetect.setHandleStatus(0);
        if (confidence >= 80){
            liveDetect.setHandleStatus(1);
            liveDetect.setResumeTime(LocalDateTime.now().plusHours(8));
            // 封号处理
            tencentLiveService.ban(rid, null);
        }
        liveDetectService.save(liveDetect);

    }

}