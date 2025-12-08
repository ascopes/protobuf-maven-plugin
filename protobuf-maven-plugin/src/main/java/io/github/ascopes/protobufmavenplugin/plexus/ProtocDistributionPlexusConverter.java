/*
 * Copyright (C) 2023 - 2025, Ashley Scopes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.ascopes.protobufmavenplugin.plexus;

import io.github.ascopes.protobufmavenplugin.dependencies.PlatformClassifierFactory;
import io.github.ascopes.protobufmavenplugin.protoc.dists.BinaryMavenProtocDistributionBean;
import io.github.ascopes.protobufmavenplugin.protoc.dists.PathProtocDistributionBean;
import io.github.ascopes.protobufmavenplugin.protoc.dists.ProtocDistribution;
import io.github.ascopes.protobufmavenplugin.protoc.dists.UriProtocDistributionBean;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import java.net.URI;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.basic.AbstractBasicConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.eclipse.sisu.Description;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Description(
    "Converter for protoc distribution parsing to enable backwards support of old "
        + "string-based versions"
)
@Singleton
@Named
public class ProtocDistributionPlexusConverter
    extends AbstractBasicConverter
    implements PlexusConverter {

  private static final Logger log = LoggerFactory
      .getLogger(ProtocDistributionPlexusConverter.class);

  private final SealedTypePlexusConverter sealedTypePlexusConverter;
  private final PlatformClassifierFactory platformClassifierFactory;

  @Inject
  ProtocDistributionPlexusConverter(
      SealedTypePlexusConverter sealedTypePlexusConverter,
      PlatformClassifierFactory platformClassifierFactory
  ) {
    this.sealedTypePlexusConverter = sealedTypePlexusConverter;
    this.platformClassifierFactory = platformClassifierFactory;
  }

  @Override
  public int getOrder() {
    return 0;
  }

  @Override
  public boolean canConvert(Class<?> type) {
    return ProtocDistribution.class.equals(type);
  }

  @Override
  public Object fromConfiguration(
      ConverterLookup lookup,
      PlexusConfiguration configuration,
      Class<?> type,
      @Nullable Class<?> enclosingType,
      ClassLoader loader,
      ExpressionEvaluator evaluator,
      @Nullable ConfigurationListener listener
  ) throws ComponentConfigurationException {
    var value = configuration.getValue();
    if (value != null) {
      log.trace("Delegating conversion of protoc distribution to string converter");
      return fromString(value);
    }

    log.trace("Delegating conversion of protoc distribution to sealed converter");
    return sealedTypePlexusConverter.fromConfiguration(
        lookup,
        configuration,
        type,
        enclosingType,
        loader,
        evaluator,
        listener
    );
  }

  @Override
  public ProtocDistribution fromString(String str) {
    log.trace("Parsing string \"{}\" as protoc distribution", str);

    if (str.equals("PATH")) {
      var bean = new PathProtocDistributionBean();
      bean.setName("protoc");
      return bean;
    }

    if (str.contains(":")) {
      var bean = new UriProtocDistributionBean();
      bean.setUrl(URI.create(str));
      return bean;
    }

    try {
      var bean = new BinaryMavenProtocDistributionBean();
      bean.setGroupId("com.google.protobuf");
      bean.setArtifactId("protoc");
      bean.setVersion(str);
      bean.setType("exe");
      bean.setClassifier(platformClassifierFactory.getClassifier("protoc"));
      return bean;
    } catch (ResolutionException ex) {
      throw new IllegalStateException(ex.toString(), ex);
    }
  }
}
