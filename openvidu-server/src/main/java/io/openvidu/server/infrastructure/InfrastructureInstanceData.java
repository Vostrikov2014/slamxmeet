//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.openvidu.server.infrastructure;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InfrastructureInstanceData {
    protected ConcurrentHashMap<String, Instance> instances = new ConcurrentHashMap();

    public InfrastructureInstanceData() {
    }

    public Collection<Instance> getInstancesCollection() {
        return this.instances.values();
    }

    public Instance getInstance(String instanceId) {
        return (Instance)this.instances.get(instanceId);
    }

    protected void addInstance(Instance instance) {
        this.instances.put(instance.getId(), instance);
    }

    protected Instance removeInstance(String instanceId) {
        return (Instance)this.instances.remove(instanceId);
    }

    public Instance getInstanceByIp(String ip) {
        return (Instance)this.instances.values().stream().filter((instance) -> {
            return ip.equals(instance.getIp());
        }).findFirst().orElse((Object)null);
    }

    public Iterator<Map.Entry<String, Instance>> getInstancesEntriesIterator() {
        return this.instances.entrySet().iterator();
    }

    public int getNumberOfInstances() {
        return this.instances.size();
    }
}
