package com.uih.uplus.solar.equipment.management.service.grpc;

import com.uih.uplus.common.log.common.UPlusLevelCode;
import com.uih.uplus.solar.equipment.management.entity.Device;
import com.uih.uplus.solar.equipment.management.model.view.DeviceInfoForSendMessage;
import com.uih.uplus.solar.equipment.management.model.view.DeviceMRLogModel;
import com.uih.uplus.solar.equipment.management.service.DeviceService;
import com.uih.uplus.solar.equipment.management.service.impl.CustomerUtils;
import com.uih.uplus.solar.equipment.shared.grpc.device.*;
import com.uih.uplus.solar.shared.Model.AuthCustomerModel;
import com.uih.uplus.solar.shared.Model.UserModel;
import com.uih.uplus.solar.shared.enums.LogCode;
import com.uih.uplus.solar.shared.utils.LogUtil;
import com.uih.uplus.solar.shared.utils.UserUtils;
import io.grpc.stub.StreamObserver;
import lombok.extern.uih.log.UIHLog;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: DeviceGrpcServiceImpl
 * @Description:
 * @Author: liang.yan@united-imging.com
 * @date: 2019/12/31
 */
@GrpcService
@UIHLog
public class DeviceGrpcServiceImpl extends DeviceServerGrpc.DeviceServerImplBase {
    @Autowired
    private DeviceService deviceService;

    @Autowired
    private UserUtils userUtils;

    @Value("${app.logical-code}")
    private String logicalCode;

    @Autowired
    private CustomerUtils customerUtils;

    @Override
    public void getSystemIdByPnAndSnNumber(DeviceQueryPro request, StreamObserver<ResultSystemIdPro> response) {
        String productNumber = request.getProductNumber();
        String serialNumber = request.getSerialNumber();
        Device device = deviceService.getBySnAndPnNumber(serialNumber, productNumber);

        String systemId = device == null ? "" : device.getSystemId();

        ResultSystemIdPro resultSystemIdPro = ResultSystemIdPro.newBuilder().setSystemId(systemId).build();
        response.onNext(resultSystemIdPro);
        response.onCompleted();
    }

    @Override
    public void queryDeviceList(DeviceParamPro request,StreamObserver<DeviceProList> response){
        List<DeviceMRLogModel> list = deviceService.queryDeviceList(request.getProductTypeName(),request.getCustomerId());
        //从租户平台获取机构名称
        if(!CollectionUtils.isEmpty(list)){
            List<Long> customerIds = list.stream().map(DeviceMRLogModel::getCustomerId).distinct().collect(Collectors.toList());
            List<AuthCustomerModel> customerModels = customerUtils.authQueryByIds(customerIds);
            for(DeviceMRLogModel deviceModel:list){
                List<AuthCustomerModel> resultList = customerModels.stream().filter(a->a.getOrgId().equals(deviceModel.getCustomerId())).collect(Collectors.toList());
                if(!CollectionUtils.isEmpty(resultList)){
                    deviceModel.setCustomerName(resultList.get(0).getName());
                }
            }
        }

        DeviceProList.Builder deviceList = DeviceProList.newBuilder();
        for (DeviceMRLogModel item : list) {
            DeviceProList.DevicePro devicePro = DeviceProList.DevicePro.newBuilder().
                    setSystemId(StringUtils.isEmpty(item.getSystemId())?"":item.getSystemId()).
                    setSerialNumber(StringUtils.isEmpty(item.getSerialNumber())?"":item.getSerialNumber()).
                    setCustomerName(StringUtils.isEmpty(item.getCustomerName())?"":item.getCustomerName()).
                    build();
            deviceList.addDevices(devicePro);
        }
        response.onNext(deviceList.build());
        response.onCompleted();
    }

    /**
     * @Description: 根据systemId查询设备相关信息
     * @parameter: request
     * @parameter: responseObserver
     * @return: void
     */
    @Override
    public void getDeviceInfoBySystemId (SystemIdPro request, StreamObserver<DeviceInfoPro> responseObserver){
        DeviceInfoForSendMessage deviceInfoForSendMessage=deviceService.getDeviceInfoForSendMessage(request.getSystemId());
        DeviceInfoPro.Builder deviceInfoPro=DeviceInfoPro.newBuilder();
        if(deviceInfoForSendMessage!=null){
            if(deviceInfoForSendMessage.getFirstEngineerId()!=null){
                deviceInfoPro.setFirstEngineerId(deviceInfoForSendMessage.getFirstEngineerId());
            }
            if(deviceInfoForSendMessage.getDeviceTypeName()!=null){
                deviceInfoPro.setDeviceTypeName(deviceInfoForSendMessage.getDeviceTypeName());
            }
            if(deviceInfoForSendMessage.getDeviceId()!=null){
                deviceInfoPro.setDeviceId(deviceInfoForSendMessage.getDeviceId());
            }
            if(deviceInfoForSendMessage.getSystemType()!=null){
                deviceInfoPro.setSystemType(deviceInfoForSendMessage.getSystemType());
            }
            if(deviceInfoForSendMessage.getDeviceTypeId()!=null){
                deviceInfoPro.setDeviceTypeId(deviceInfoForSendMessage.getDeviceTypeId());
            }
            UserModel user = userUtils.getUserById(deviceInfoForSendMessage.getFirstEngineerId());
            if(user!=null){
                if(user.getName()!=null){
                    deviceInfoPro.setFirstEngineerName(user.getName());
                }
                if(user.getOrgName()!=null){
                    deviceInfoPro.setFirstEngineerDescription(user.getOrgName());
                }
                if(user.getEmail()!=null){
                    deviceInfoPro.setFirstEngineerEmail(user.getEmail());
                }
                if(user.getPhoneNo()!=null){
                    deviceInfoPro.setFirstEngineerTel(user.getPhoneNo());
                }
            }
            //从租户平台获取机构名称
            if(deviceInfoForSendMessage.getCustomerId()!=null){
                deviceInfoPro.setCustomerId(deviceInfoForSendMessage.getCustomerId());
                List<Long> ids = new ArrayList<>();
                ids.add(deviceInfoForSendMessage.getCustomerId());
                List<AuthCustomerModel> customerModels = customerUtils.authQueryByIds(ids);
                if(!CollectionUtils.isEmpty(customerModels)){
                    deviceInfoPro.setCustomerName(customerModels.get(0).getName());
                }
            }
        }else {
            logger.log(LogUtil.createLogUid(UPlusLevelCode.LOG_DEV_ERROR, logicalCode, LogCode.DATABASE_QUERY_IS_EMPTY.getCode()), "systemId{}{}",request.getSystemId(),LogCode.DATABASE_QUERY_IS_EMPTY);
            deviceInfoPro.setDeviceTypeName("未知设备类型");
        }
        responseObserver.onNext(deviceInfoPro.build());
        responseObserver.onCompleted();
    }
}
