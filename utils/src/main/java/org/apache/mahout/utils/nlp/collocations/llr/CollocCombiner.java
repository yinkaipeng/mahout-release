/**
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

package org.apache.mahout.utils.nlp.collocations.llr;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

/** Combiner for pass1 of the CollocationDriver */
public class CollocCombiner extends MapReduceBase implements Reducer<Gram,Gram,Gram,Gram> {
  
  /**
   * collocation finder: pass 1 colloc phase:
   * 
   * given input from the mapper, k:h_subgram:1 v:ngram:1 k:t_subgram:1 v:ngram:1
   * 
   * count ngrams and subgrams.
   * 
   * output is:
   * 
   * k:h_subgram:subgramfreq v:ngram:ngramfreq k:t_subgram:subgramfreq v:ngram:ngramfreq
   * 
   * Each ngram's frequency is essentially counted twice, frequency should be the same for the head and tail.
   * Fix this to count only for the head and move the count into the value?
   */
  @Override
  public void reduce(Gram subgramKey,
                     Iterator<Gram> ngramValues,
                     OutputCollector<Gram,Gram> output,
                     Reporter reporter) throws IOException {
    
    HashMap<Gram,Gram> ngramSet = new HashMap<Gram,Gram>();
    int subgramFrequency = 0;
    
    while (ngramValues.hasNext()) {
      Gram ngram = ngramValues.next();
      subgramFrequency += ngram.getFrequency();
      
      Gram ngramCanon = ngramSet.get(ngram);
      if (ngramCanon == null) {
        // t is potentially reused, so create a new object to populate the HashMap
        Gram ngramEntry = new Gram(ngram);
        ngramSet.put(ngramEntry, ngramEntry);
      } else {
        ngramCanon.incrementFrequency(ngram.getFrequency());
      }
    }
    
    // emit subgram:subgramFreq ngram:ngramFreq pairs
    subgramKey.setFrequency(subgramFrequency);
    
    for (Gram ngram : ngramSet.keySet()) {
      output.collect(subgramKey, ngram);
    }
  }
  
}
