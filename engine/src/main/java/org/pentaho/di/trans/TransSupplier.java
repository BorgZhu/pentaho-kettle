/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2018 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.trans;

import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.plugins.PluginInterface;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.trans.ael.adapters.DataflowEngineAdapter;
import org.pentaho.di.trans.ael.websocket.TransWebSocketEngineAdapter;

import java.util.function.Supplier;

public class TransSupplier implements Supplier<Trans> {

  private final TransMeta transMeta;
  private final LogChannelInterface log;
  private final Supplier<Trans> fallbackSupplier;

  public TransSupplier( TransMeta transMeta, LogChannelInterface log, Supplier<Trans> fallbackSupplier ) {
    this.transMeta = transMeta;
    this.log = log;
    this.fallbackSupplier = fallbackSupplier;
  }

  /**
   * Creates the appropriate trans.  Either
   * 1)  A {@link TransWebSocketEngineAdapter} wrapping an Engine
   * if an alternate execution engine has been selected
   * 2)  A legacy {@link Trans} otherwise.
   */
  public Trans get() {
    if ( Utils.isEmpty( transMeta.getVariable( "engine" ) ) ) {
      log.logBasic( "Using legacy execution engine" );
      return fallbackSupplier.get();
    }

    Variables variables = new Variables();
    variables.initializeVariablesFrom( null );
    String protocol = transMeta.getVariable( "engine.protocol" );
    String host = transMeta.getVariable( "engine.host" );
    String port = transMeta.getVariable( "engine.port" );

    // TODO This should point to an OSGi Service for the plugable Engine Implementation
    String remoteEngine = transMeta.getVariable( "engine.remote" );
    if ( remoteEngine.equalsIgnoreCase( "spark" ) ) {
      //default value for ssl for now false
      boolean ssl = "https".equalsIgnoreCase( protocol ) || "wss".equalsIgnoreCase( protocol );
      return new TransWebSocketEngineAdapter( transMeta, host, port, ssl );

    } else {
      return new DataflowEngineAdapter( transMeta );
    }
  }

  private Object loadPlugin( PluginInterface plugin ) {
    try {
      return PluginRegistry.getInstance().loadClass( plugin );
    } catch ( KettlePluginException e ) {
      throw new RuntimeException( e );
    }
  }
}
