package com.pipeline.consul.devservices;

import org.jboss.logging.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages IP address allocation for containers in the dev services network.
 * Allocates IPs from a subnet (default 10.5.0.0/24) with up to 254 addresses.
 */
public class IPAllocator {
    
    private static final Logger LOG = Logger.getLogger(IPAllocator.class);
    
    private final String subnet;
    private final String baseIP;
    private final int maxHosts;
    private final AtomicInteger nextIP = new AtomicInteger(2); // Start at .2 (.1 is gateway)
    private final Set<String> allocatedIPs = ConcurrentHashMap.newKeySet();
    
    public IPAllocator(String subnet) {
        this.subnet = subnet;
        
        // Extract base IP from subnet (e.g., "10.5.0.0/24" -> "10.5.0")
        String[] parts = subnet.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid subnet format. Expected format: x.x.x.x/mask");
        }
        
        String[] ipParts = parts[0].split("\\.");
        if (ipParts.length != 4) {
            throw new IllegalArgumentException("Invalid IP format in subnet");
        }
        
        this.baseIP = ipParts[0] + "." + ipParts[1] + "." + ipParts[2];
        
        // Calculate max hosts based on subnet mask
        int maskBits = Integer.parseInt(parts[1]);
        this.maxHosts = (int) Math.pow(2, 32 - maskBits) - 2; // -2 for network and broadcast
        
        LOG.infof("IPAllocator initialized with subnet %s, supporting %d hosts", subnet, maxHosts);
    }
    
    /**
     * Allocate the next available IP address.
     * 
     * @return The allocated IP address
     * @throws IllegalStateException if no more IPs are available
     */
    public synchronized String allocateIP() {
        int current = nextIP.getAndIncrement();
        
        if (current > maxHosts) {
            throw new IllegalStateException("No more IP addresses available in subnet " + subnet);
        }
        
        String ip = baseIP + "." + current;
        allocatedIPs.add(ip);
        
        LOG.debugf("Allocated IP: %s", ip);
        return ip;
    }
    
    /**
     * Allocate a specific IP address.
     * 
     * @param hostPart The last octet of the IP (e.g., 10 for x.x.x.10)
     * @return The allocated IP address
     * @throws IllegalArgumentException if the IP is invalid or already allocated
     */
    public synchronized String allocateSpecificIP(int hostPart) {
        if (hostPart < 2 || hostPart > maxHosts) {
            throw new IllegalArgumentException("Host part must be between 2 and " + maxHosts);
        }
        
        String ip = baseIP + "." + hostPart;
        
        if (allocatedIPs.contains(ip)) {
            throw new IllegalArgumentException("IP " + ip + " is already allocated");
        }
        
        allocatedIPs.add(ip);
        
        // Update nextIP if necessary
        if (hostPart >= nextIP.get()) {
            nextIP.set(hostPart + 1);
        }
        
        LOG.debugf("Allocated specific IP: %s", ip);
        return ip;
    }
    
    /**
     * Release an allocated IP address.
     * 
     * @param ip The IP to release
     */
    public synchronized void releaseIP(String ip) {
        if (allocatedIPs.remove(ip)) {
            LOG.debugf("Released IP: %s", ip);
        }
    }
    
    /**
     * Get the gateway IP for the subnet (always .1).
     */
    public String getGatewayIP() {
        return baseIP + ".1";
    }
    
    /**
     * Get the subnet in CIDR notation.
     */
    public String getSubnet() {
        return subnet;
    }
    
    /**
     * Get the number of allocated IPs.
     */
    public int getAllocatedCount() {
        return allocatedIPs.size();
    }
    
    /**
     * Check if an IP is allocated.
     */
    public boolean isAllocated(String ip) {
        return allocatedIPs.contains(ip);
    }
    
    /**
     * Reset the allocator, releasing all IPs.
     */
    public synchronized void reset() {
        allocatedIPs.clear();
        nextIP.set(2);
        LOG.debug("IPAllocator reset");
    }
}