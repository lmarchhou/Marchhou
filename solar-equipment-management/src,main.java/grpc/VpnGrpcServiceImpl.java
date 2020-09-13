package com.uih.uplus.solar.equipment.management.service.grpc;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.protobuf.Empty;
import com.uih.uplus.solar.equipment.management.entity.Vpn;
import com.uih.uplus.solar.equipment.management.service.VpnService;
import com.uih.uplus.solar.equipment.shared.grpc.vpn.VpnConnectStatus;
import com.uih.uplus.solar.equipment.shared.grpc.vpn.VpnIdIplist;
import com.uih.uplus.solar.equipment.shared.grpc.vpn.VpnServerGrpc;
import com.uih.uplus.solar.equipment.shared.grpc.vpn.VpnStatusResult;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * @author zhou.yang
 * @ClassName: VpnGrpcServiceImpl
 * @Description: VPN的grpc服务类
 * @date 2020/1/4
 */
@GrpcService
public class VpnGrpcServiceImpl extends VpnServerGrpc.VpnServerImplBase {
    @Autowired
    private VpnService vpnService;

    @Override
    public void monitorList(Empty request, StreamObserver<VpnIdIplist> responseObserver) {
        QueryWrapper<Vpn> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("IP_ADDRESS","");
        queryWrapper.eq("DELETED","0");
        List<Vpn> vpns = vpnService.list(queryWrapper);
        VpnIdIplist.Builder vpnIdIplistBuilder = VpnIdIplist.newBuilder();
        for(Vpn item:vpns){
            VpnIdIplist.VpnIdIp vpnIdIp = VpnIdIplist.VpnIdIp.newBuilder()
                    .setVpnId(item.getId())
                    .setVpnIp(item.getIpAddress())
                    .build();
            vpnIdIplistBuilder.addVpnIdIps(vpnIdIp);
        }
        responseObserver.onNext(vpnIdIplistBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateStatus(VpnConnectStatus request, StreamObserver<VpnStatusResult> responseObserver){
        Vpn vpn = new Vpn();
        vpn.setConnectStatus(request.getStatus());
        vpn.setConnectUpdateDatetime(new Date());
        vpn.setId(request.getVpnId());
        boolean flag = vpnService.updateById(vpn);
        VpnStatusResult result = VpnStatusResult.newBuilder().setResult(flag).build();
        responseObserver.onNext(result);
        responseObserver.onCompleted();
    }


}
