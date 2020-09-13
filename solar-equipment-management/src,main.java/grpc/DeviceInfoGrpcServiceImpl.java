package com.uih.uplus.solar.equipment.management.service.grpc;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.protobuf.Empty;
import com.uih.uplus.solar.equipment.management.entity.DeviceInfo;
import com.uih.uplus.solar.equipment.management.service.DeviceInfoService;
import com.uih.uplus.solar.equipment.management.service.DevicePeriodStatusService;
import com.uih.uplus.solar.equipment.shared.grpc.deviceinfo.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: DeviceInfoGrpcServiceImpl
 * @Description:
 * @Author: liang.yan@united-imging.com
 * @date: 2019/12/31
 *
 *
 */
@GrpcService
public class DeviceInfoGrpcServiceImpl extends DeviceInfoServerGrpc.DeviceInfoServerImplBase{
    @Autowired
    private DeviceInfoService deviceInfoService;
    @Autowired
    DevicePeriodStatusService devicePeriodStatusService;

    @Override
    public void judgeIpExist(IpAddressPro request, StreamObserver<ResultFlagPro> responseObserver){
        QueryWrapper<DeviceInfo> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("IP_ADDRESS",request.getIpAddress()).or().eq("IACU_IP_ADDRESS",request.getIpAddress());
        List<DeviceInfo> deviceInfos=deviceInfoService.list(queryWrapper);
        Boolean ipExist= !CollectionUtils.isEmpty(deviceInfos);
        ResultFlagPro resultFlagPro=ResultFlagPro.newBuilder().setFlag(ipExist).build();
        responseObserver.onNext(resultFlagPro);
        responseObserver.onCompleted();
    }

    @Override
    public void monitorList(Empty request, StreamObserver<DeviceIdIplist> responseObserver){
        QueryWrapper<DeviceInfo> queryWrapper=new QueryWrapper<>();
        queryWrapper.ne("IP_ADDRESS","");
        queryWrapper.eq("DELETED","0");
        List<DeviceInfo> deviceInfos = deviceInfoService.list(queryWrapper);
        DeviceIdIplist.Builder deviceIdIplistBuilder = DeviceIdIplist.newBuilder();
        for(DeviceInfo item : deviceInfos){
            DeviceIdIplist.DeviceIdIp deviceIdIp = DeviceIdIplist.DeviceIdIp.newBuilder()
                    .setDeviceId(item.getDeviceId())
                    .setDeviceIp(item.getIpAddress())
                    .build();
            deviceIdIplistBuilder.addDeviceIdIps(deviceIdIp);
        }
        responseObserver.onNext(deviceIdIplistBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateStatus(DeviceConnectStatus request, StreamObserver<ResultFlagPro> responseObserver){
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceId(request.getDeviceId());
        deviceInfo.setConnectStatus(request.getStatus());
        deviceInfo.setConnectUpdateDatetime(new Date());
        boolean flag = deviceInfoService.updateByDeviceId(deviceInfo);
        ResultFlagPro resultFlagPro = ResultFlagPro.newBuilder().setFlag(flag).build();
        responseObserver.onNext(resultFlagPro);
        responseObserver.onCompleted();
    }

    @Override
    public void savePeriodStatus(Empty request, StreamObserver<Empty> responseObserver) {
        devicePeriodStatusService.savePeriodStatus();
        responseObserver.onCompleted();
    }
}
