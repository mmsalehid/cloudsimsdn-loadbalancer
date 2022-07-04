package org.cloudbus.cloudsim.sdn.policies.vmallocation;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.sdn.CloudletSchedulerSpaceSharedMonitor;
import org.cloudbus.cloudsim.sdn.Configuration;
import org.cloudbus.cloudsim.sdn.nos.NetworkOperatingSystem;
import org.cloudbus.cloudsim.sdn.virtualcomponents.SDNVm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class VmForwarder{

    NetworkOperatingSystem nos;
    Map <Integer, List<Integer>> vmPool;
    Map <Integer, Integer> vmToOrgVmMap;


    public VmForwarder(NetworkOperatingSystem nos){
        this.nos = nos;
        vmPool = new HashMap<>();
        vmToOrgVmMap = new HashMap<>();
    }

    public SDNVm createAndAddVmReplica(SDNVm orgVm, String newVmHost){
        SDNVm newVM = this.duplicateVm(orgVm, newVmHost);
        this.addDuplicateVm(orgVm.getId(), newVM);
        this.nos.addExtraVm(newVM, this.nos );
        return newVM;
    }

    private boolean addDuplicateVm(Integer orgVmId, SDNVm newVm) {
        if (vmToOrgVmMap.containsKey(newVm.getId())){
            return false;
        }
        else{
            vmToOrgVmMap.put(newVm.getId(), orgVmId);
        }
        if (vmPool.containsKey(orgVmId)) {
            List<Integer> vms = vmPool.get(orgVmId);
            vms.add(newVm.getId());
        } else {
            vmToOrgVmMap.put(orgVmId, orgVmId);
            List<Integer> orgVmReplicas = new ArrayList<>();
            orgVmReplicas.add(orgVmId);
            orgVmReplicas.add(newVm.getId());
            vmPool.put(orgVmId, orgVmReplicas);
        }
        return true;
    }


    private SDNVm duplicateVm(SDNVm orgVm, String newVmHostName){
        CloudletScheduler clSch = new CloudletSchedulerSpaceSharedMonitor(Configuration.TIME_OUT);
        int newId = SDNVm.getUniqueVmId();
        SDNVm newVm = new SDNVm(
                newId,
                orgVm.getUserId(), orgVm.getMips(), orgVm.getNumberOfPes(), orgVm.getRam(),
                orgVm.getBw(), orgVm.getSize(), orgVm.getVmm(), clSch,
                orgVm.getStartTime(), orgVm.getFinishTime());

        newVm.setName(orgVm.getName()+"-dup"+orgVm.getId() + "-id" + newId);
        newVm.setMiddleboxType(orgVm.getMiddleboxType());
        newVm.setHostName(newVmHostName);

        return newVm;
    }


    public void processVmCreateDynamicAck(SDNVm newVm) {
        int orgVmId = vmToOrgVmMap.get(newVm.getId());
        nos.addExtraPath(orgVmId,newVm.getId());
    }

    public int getNextRandomDestinationVm(int destinationVmId) {
        Random randomGenerator = new Random();
        int numOfReplicas = vmPool.get(destinationVmId).size();
        List<Integer> availableVmList;
        if (numOfReplicas == 0){
            availableVmList = new ArrayList<>(vmToOrgVmMap.keySet());
            numOfReplicas = availableVmList.size();
        }
        else{
            availableVmList = vmPool.get(destinationVmId);
        }
        int newDestIndex = randomGenerator.nextInt(numOfReplicas);
        int newDestinationVmId = availableVmList.get(newDestIndex);
        return newDestinationVmId;
    }

    public void updateVmPool(List <Integer> overloadedVmList, List<Integer> underloadedVmList) {
        for(Integer vmId: overloadedVmList){
            Integer orgVm = vmToOrgVmMap.get(vmId);
            List<Integer> vms = vmPool.get(orgVm);
            vms.remove(vmId);

        }
        for (Integer vmId: underloadedVmList){
            Integer orgVm = vmToOrgVmMap.get(vmId);
            List <Integer> vms = vmPool.get(orgVm);
            if (!vms.contains(vmId)){
                vms.add(vmId);
            }
        }
        Log.printLine(vmPool);
    }
}
