/*
 * Copyright (c) 2005, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.File;
import org.dacapo.lusearch.Search;

import org.dacapo.parser.Config;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Lusearch.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class LusearchHarness extends org.dacapo.harness.Benchmark {
  private final Search benchmark;

  public LusearchHarness(Config config, File scratch) throws Exception {
    super(config, scratch, false);
    benchmark = new Search();
  }

  @Override
  public void iterate(String size) throws Exception {
    benchmark.main(config.preprocessArgs(size, scratch));
  }
}
