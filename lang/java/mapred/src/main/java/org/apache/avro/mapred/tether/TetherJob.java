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

package org.apache.avro.mapred.tether;

import java.io.IOException;
import java.util.Collection;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RunningJob;

/** Constructs and submits tether jobs. This may be used as an API-based
 *  method to launch tether jobs. */
@SuppressWarnings("deprecation")
public class TetherJob extends Configured {

  /** Get the URI of the application's executable. */
  public static URI getExecutable(JobConf job) {
    try {
      return new URI(job.get("avro.tether.executable"));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
  
  /** Set the URI for the application's executable. Normally this in HDFS. */
  public static void setExecutable(JobConf job, URI executable) {
    job.set("avro.tether.executable", executable.toString());
  }

  /** Submit a job to the map/reduce cluster. All of the necessary
   * modifications to the job to run under tether are made to the
   * configuration.
   */
  public static RunningJob runJob(JobConf job) throws IOException {
    setupTetherJob(job);
    return JobClient.runJob(job);
  }

  /** Submit a job to the Map-Reduce framework. */
  public static RunningJob submitJob(JobConf conf) throws IOException {
    setupTetherJob(conf);
    return new JobClient(conf).submitJob(conf);
  }
  
  private static void setupTetherJob(JobConf job) throws IOException {
    job.setMapRunnerClass(TetherMapRunner.class);
    job.setPartitionerClass(TetherPartitioner.class);
    job.setReducerClass(TetherReducer.class);

    job.setInputFormat(TetherInputFormat.class);
    job.setOutputFormat(TetherOutputFormat.class);

    job.setOutputKeyClass(TetherData.class);
    job.setOutputKeyComparatorClass(TetherKeyComparator.class);
    job.setMapOutputValueClass(NullWritable.class);

    // add TetherKeySerialization to io.serializations
    Collection<String> serializations =
      job.getStringCollection("io.serializations");
    if (!serializations.contains(TetherKeySerialization.class.getName())) {
      serializations.add(TetherKeySerialization.class.getName());
      job.setStrings("io.serializations",
                     serializations.toArray(new String[0]));
    }
    
    DistributedCache.addCacheFile(getExecutable(job), job);
  }

}
