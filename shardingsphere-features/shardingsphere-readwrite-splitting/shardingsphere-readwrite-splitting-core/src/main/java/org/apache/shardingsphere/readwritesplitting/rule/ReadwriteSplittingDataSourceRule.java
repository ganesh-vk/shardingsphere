/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.readwritesplitting.rule;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.shardingsphere.infra.distsql.constant.ExportableConstants;
import org.apache.shardingsphere.readwritesplitting.api.rule.ReadwriteSplittingDataSourceRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.spi.ReplicaLoadBalanceAlgorithm;
import org.apache.shardingsphere.readwritesplitting.type.ReadwriteSplittingDataSourceProcessor;
import org.apache.shardingsphere.readwritesplitting.type.ReadwriteSplittingDataSourceProcessorFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Readwrite-splitting data source rule.
 */
@Getter
public final class ReadwriteSplittingDataSourceRule {
    
    private final String name;
    
    private final ReplicaLoadBalanceAlgorithm loadBalancer;
    
    private final ReadwriteSplittingDataSourceProcessor dataSourceProcessor;
    
    @Getter(AccessLevel.NONE)
    private final Collection<String> disabledDataSourceNames = new HashSet<>();
    
    public ReadwriteSplittingDataSourceRule(final ReadwriteSplittingDataSourceRuleConfiguration config, final ReplicaLoadBalanceAlgorithm loadBalancer) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(config.getName()), "Name is required.");
        name = config.getName();
        this.loadBalancer = loadBalancer;
        dataSourceProcessor = ReadwriteSplittingDataSourceProcessorFactory.newInstance(config.getType(), config.getProps());
    }
    
    /**
     * Get write data source name.
     *
     * @return write data source name
     */
    public String getWriteDataSource() {
        return dataSourceProcessor.getWriteDataSource();
    }
    
    /**
     * Get read data source names.
     *
     * @return available read data source names
     */
    public List<String> getReadDataSourceNames() {
        return dataSourceProcessor.getReadDataSources().stream().filter(each -> !disabledDataSourceNames.contains(each)).collect(Collectors.toList());
    }
    
    /**
     * Update disabled data source names.
     *
     * @param dataSourceName data source name
     * @param isDisabled is disabled
     */
    public void updateDisabledDataSourceNames(final String dataSourceName, final boolean isDisabled) {
        if (isDisabled) {
            disabledDataSourceNames.add(dataSourceName);
        } else {
            disabledDataSourceNames.remove(dataSourceName);
        }
    }
    
    /**
     * Get data source mapper.
     *
     * @return data source mapper
     */
    public Map<String, Collection<String>> getDataSourceMapper() {
        return dataSourceProcessor.getDataSourceMapper(name);
    }
    
    /**
     * Get data sources.
     *
     * @param removeDisabled whether to remove the disabled resource
     * @return data sources
     */
    public Map<String, String> getDataSources(final boolean removeDisabled) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Entry<String, String> entry : dataSourceProcessor.getDataSources().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (ExportableConstants.REPLICA_DATA_SOURCE_NAMES.equals(key) && removeDisabled) {
                value = removeDisabledDataSources(value);
            }
            result.put(key, value);
        }
        return result;
    }
    
    private String removeDisabledDataSources(final String readDataSources) {
        if (disabledDataSourceNames.isEmpty()) {
            return readDataSources;
        }
        Collection<String> dataSources = new LinkedList<>(Splitter.on(",").trimResults().splitToList(readDataSources));
        dataSources.removeIf(disabledDataSourceNames::contains);
        return String.join(",", dataSources);
    }
}
