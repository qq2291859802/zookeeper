/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.server;



import org.apache.zookeeper.common.Time;
import org.apache.zookeeper.server.quorum.BufferStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Basic Server Statistics
 *
 * 服务器状态，队列数量，client存活数量等信息属性，函数完成延迟，发送，接收量等信息
 */
public class ServerStats {
    private static final Logger LOG = LoggerFactory.getLogger(ServerStats.class);

    private long packetsSent;//发送包个数
    private long packetsReceived;//接收个数
    private long maxLatency;//最长延迟
    private long minLatency = Long.MAX_VALUE;//最短延迟
    private long totalLatency = 0;//总延迟
    private long count = 0;//延迟次数
    // 超过实时刷新日志数据耗时（可配置）的次数
    private AtomicLong fsyncThresholdExceedCount = new AtomicLong(0);

    private final BufferStats clientResponseStats = new BufferStats();

    private final Provider provider;

    public interface Provider {
        public long getOutstandingRequests();
        public long getLastProcessedZxid();
        public String getState();
        public int getNumAliveConnections();
        public long getDataDirSize();
        public long getLogDirSize();
    }
    
    public ServerStats(Provider provider) {
        this.provider = provider;
    }
    
    // getters
    synchronized public long getMinLatency() {
        return minLatency == Long.MAX_VALUE ? 0 : minLatency;
    }

    synchronized public long getAvgLatency() {
        if (count != 0) {
            return totalLatency / count;
        }
        return 0;
    }

    synchronized public long getMaxLatency() {
        return maxLatency;
    }

    public long getOutstandingRequests() {
        return provider.getOutstandingRequests();
    }
    
    public long getLastProcessedZxid(){
        return provider.getLastProcessedZxid();
    }

    public long getDataDirSize() {
        return provider.getDataDirSize();
    }

    public long getLogDirSize() {
        return provider.getLogDirSize();
    }
    
    synchronized public long getPacketsReceived() {
        return packetsReceived;
    }

    synchronized public long getPacketsSent() {
        return packetsSent;
    }

    public String getServerState() {
        return provider.getState();
    }
    
    /** The number of client connections alive to this server */
    public int getNumAliveClientConnections() {
    	return provider.getNumAliveConnections();
    }

    public boolean isProviderNull() {
        return provider == null;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Latency min/avg/max: " + getMinLatency() + "/"
                + getAvgLatency() + "/" + getMaxLatency() + "\n");
        sb.append("Received: " + getPacketsReceived() + "\n");
        sb.append("Sent: " + getPacketsSent() + "\n");
        sb.append("Connections: " + getNumAliveClientConnections() + "\n");

        if (provider != null) {
            sb.append("Outstanding: " + getOutstandingRequests() + "\n");
            sb.append("Zxid: 0x"+ Long.toHexString(getLastProcessedZxid())+ "\n");
        }
        sb.append("Mode: " + getServerState() + "\n");
        return sb.toString();
    }
    // mutators

    /**
     * 更新延时
     * @param requestCreateTime
     */
    synchronized void updateLatency(long requestCreateTime) {
        long latency = Time.currentElapsedTime() - requestCreateTime;
        totalLatency += latency;
        count++;
        if (latency < minLatency) {
            minLatency = latency;
        }
        if (latency > maxLatency) {
            maxLatency = latency;
        }
    }
    synchronized public void resetLatency(){
        totalLatency = 0;
        count = 0;
        maxLatency = 0;
        minLatency = Long.MAX_VALUE;
    }
    synchronized public void resetMaxLatency(){
        maxLatency = getMinLatency();
    }
    synchronized public void incrementPacketsReceived() {
        packetsReceived++;
    }
    synchronized public void incrementPacketsSent() {
        packetsSent++;
    }
    synchronized public void resetRequestCounters(){
        packetsReceived = 0;
        packetsSent = 0;
    }

    public long getFsyncThresholdExceedCount() {
        return fsyncThresholdExceedCount.get();
    }

    public void incrementFsyncThresholdExceedCount() {
        fsyncThresholdExceedCount.incrementAndGet();
    }

    public void resetFsyncThresholdExceedCount() {
        fsyncThresholdExceedCount.set(0);
    }

    /**
     * 重置统计
     */
    synchronized public void reset() {
        resetLatency();
        resetRequestCounters();
        clientResponseStats.reset();
    }

    public void updateClientResponseSize(int size) {
        clientResponseStats.setLastBufferSize(size);
    }

    public BufferStats getClientResponseStats() {
        return clientResponseStats;
    }
}
