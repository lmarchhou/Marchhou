package com.uih.uplus.solar.equipment.management.service.grpc;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.uih.uplus.solar.equipment.management.entity.MonitorParameters;
import com.uih.uplus.solar.equipment.management.service.MonitorParametersService;
import com.uih.uplus.solar.equipment.shared.grpc.monitorparameters.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @ClassName: MonitorParametersServiceImpl
 * @Description:
 * @Author: liang.yan@united-imging.com
 * @date: 2020/2/24
 */
@GrpcService
public class MonitorParametersGrpcServiceImpl extends MonitorParametersServerGrpc.MonitorParametersServerImplBase {

    @Autowired
    private MonitorParametersService monitorParametersService;

    @Override
    public void getMonitorParameters(MonitorParametersQueryPro request, StreamObserver<MonitorParametersList> responseObserver) {
        QueryWrapper<MonitorParameters> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MonitorParameters::getDeviceTypeId, request.getDeviceTypeId())
                .eq(MonitorParameters::getMonitorComponent, request.getMonitorComponent())
                .eq(MonitorParameters::getMonitorValue, request.getMonitorValue());
        List<MonitorParameters> monitorParameters = monitorParametersService.list(queryWrapper);
        MonitorParametersList.Builder monitorParametersBuilder = MonitorParametersList.newBuilder();
        for (MonitorParameters item : monitorParameters) {
            MonitorParameterValue.Builder monitorParameterValue = MonitorParameterValue.newBuilder();
            monitorParameterValue.setParameterValue(item.getParameterValue());
            monitorParameterValue.setParameterCName(item.getParameterCname());
            monitorParameterValue.setParameterEName(item.getParameterEname());
            monitorParameterValue.setParameterUnit(StringUtils.isEmpty(item.getParameterUnit()) ? "" : item.getParameterUnit());
            monitorParameterValue.setParameterUpperThreshold(item.getParameterUpperThreshold() == null ? "" : item.getParameterUpperThreshold().toString());
            monitorParameterValue.setParameterLowerThreshold(item.getParameterLowerThreshold() == null ? "" : item.getParameterLowerThreshold().toString());
            monitorParameterValue.setMonitorGraphicType(StringUtils.isEmpty(item.getMonitorGraphicType())?"":item.getMonitorGraphicType());
            monitorParameterValue.setLogType(item.getLogType());
            monitorParameterValue.build();
            monitorParametersBuilder.addMonitorParameterValues(monitorParameterValue);
        }
        responseObserver.onNext(monitorParametersBuilder.build());
        responseObserver.onCompleted();
    }

    /**
     * @Description: 根据systemId获取ws 应用
     * @parameter: request
     * @parameter: responseObserver
     * @return: void
     */
    @Override
    public void getWsMonitorParametersBySystemId(SystemIdQueryPro request, StreamObserver<MonitorParametersList> responseObserver) {
        MonitorParametersList.Builder monitorParametersBuilder = MonitorParametersList.newBuilder();
        List<MonitorParameters> monitorParameters = monitorParametersService.listWsBySystemId(request.getSystemId());
        for (MonitorParameters item : monitorParameters) {
            MonitorParameterValue.Builder monitorParameterValue = MonitorParameterValue.newBuilder();
            monitorParameterValue.setParameterValue(item.getParameterValue());
            monitorParameterValue.setParameterCName(item.getParameterCname());
            monitorParameterValue.setParameterEName(item.getParameterEname());
            monitorParameterValue.setParameterUnit(StringUtils.isEmpty(item.getParameterUnit()) ? "" : item.getParameterUnit());
            monitorParameterValue.setParameterUpperThreshold(item.getParameterUpperThreshold() == null ? "" : item.getParameterUpperThreshold().toString());
            monitorParameterValue.setParameterLowerThreshold(item.getParameterLowerThreshold() == null ? "" : item.getParameterLowerThreshold().toString());
            monitorParameterValue.setMonitorGraphicType(StringUtils.isEmpty(item.getMonitorGraphicType())?"":item.getMonitorGraphicType());
            monitorParameterValue.setLogType(item.getLogType());
            monitorParameterValue.build();
            monitorParametersBuilder.addMonitorParameterValues(monitorParameterValue);
        }
        responseObserver.onNext(monitorParametersBuilder.build());
        responseObserver.onCompleted();
    }
}
