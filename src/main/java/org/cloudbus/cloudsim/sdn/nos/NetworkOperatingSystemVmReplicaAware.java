package org.cloudbus.cloudsim.sdn.nos;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.sdn.Packet;
import org.cloudbus.cloudsim.sdn.physicalcomponents.SDNHost;
import org.cloudbus.cloudsim.sdn.policies.vmallocation.VmForwarder;
import org.cloudbus.cloudsim.sdn.sfc.ServiceFunction;
import org.cloudbus.cloudsim.sdn.sfc.ServiceFunctionChainPolicy;
import org.cloudbus.cloudsim.sdn.virtualcomponents.FlowConfig;
import org.cloudbus.cloudsim.sdn.virtualcomponents.SDNVm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NetworkOperatingSystemVmReplicaAware extends NetworkOperatingSystemSimple {

    VmForwarder vmForwarder;

    public NetworkOperatingSystemVmReplicaAware(String name){
        super(name);
        this.vmForwarder = new VmForwarder(this);
    }

    @Override
    protected boolean deployApplication(List<Vm> vms, Collection<FlowConfig> links, List<ServiceFunctionChainPolicy> sfcPolicy) {
        List<SDNHost> hosts = this.getHostList();
        List<SDNVm> serverVms = new ArrayList<>();
        for (Vm vm: vms){
            String type = ((SDNVm) vm).getMiddleboxType();
            if(type.equals("server")){
                serverVms.add((SDNVm) vm);
            }
        }
        List<SDNHost> serverHosts = new ArrayList<>();
        for (SDNHost host: hosts){
            if(host.getName().startsWith("server")){
                serverHosts.add(host);
            }
        }
        int numOfReplicas = serverHosts.size()-1;
        for (SDNVm vm: serverVms){
            vm.setHostName(serverHosts.get(0).getName());
            for (int i=0;i < numOfReplicas;i++){
                SDNVm newVm = vmForwarder.createAndAddVmReplica(vm, serverHosts.get(i+1).getName());
            }
        }// update handle ack massage in nos to update channels
        return super.deployApplication(vms, links, sfcPolicy);
    }
    @Override
    protected void processVmCreateDynamicAck(SimEvent ev){
        Object [] data = (Object []) ev.getData();
        SDNVm newVm = (SDNVm) data[0];
        boolean result = (boolean) data[1];

        if(result) {
            Log.printLine(CloudSim.clock() + ": " + getName() + ".processVmCreateDynamic: Dynamic VM("+newVm+") creation succesful!");
            if(newVm instanceof ServiceFunction)
                sfcForwarder.processVmCreateDyanmicAck((ServiceFunction)newVm);
            else{
                vmForwarder.processVmCreateDynamicAck(newVm);
            }

        }
        else {
            // VM cannot be created here..
            Log.printLine(CloudSim.clock() + ": " + getName() + ".processVmCreateDynamic: Dynamic VM cannot be created!! :"+newVm);
            System.err.println(CloudSim.clock() + ": " + getName() + ".processVmCreateDynamic: Dynamic VM cannot be created!! :"+newVm);
            sfcForwarder.processVmCreateDyanmicFailed((ServiceFunction)newVm);
        }
    }

    @Override
    public Packet addPacketToChannel(Packet orgPkt){
        int destinationVmId = orgPkt.getDestination();
        int newDestinationVmId = this.vmForwarder.getNextRandomDestinationVm(destinationVmId);
        orgPkt.changeDestination(newDestinationVmId);
        return super.addPacketToChannel(orgPkt);
    }
}
