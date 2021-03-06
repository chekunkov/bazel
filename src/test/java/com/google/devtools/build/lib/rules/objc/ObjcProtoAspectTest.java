// Copyright 2017 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.rules.objc;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Iterables;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.analysis.ConfiguredTarget;
import com.google.devtools.build.lib.analysis.util.BuildViewTestCase;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.packages.util.MockObjcSupport;
import com.google.devtools.build.lib.packages.util.MockProtoSupport;
import com.google.devtools.build.lib.vfs.PathFragment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test case for the objc_proto_library aspect. */
@RunWith(JUnit4.class)
public final class ObjcProtoAspectTest extends BuildViewTestCase {

  @Before
  public final void initializeToolsConfigMock() throws Exception {
    MockProtoSupport.setup(mockToolsConfig);
    MockObjcSupport.setupObjcProto(mockToolsConfig);
  }

  @Test
  public void testObjcProtoAspectPropagatesProvider() throws Exception {
    scratch.file(
        "x/BUILD",
        "proto_library(",
        "  name = 'protos',",
        "  srcs = ['data.proto'],",
        ")",
        "objc_proto_library(",
        "  name = 'x',",
        "  deps = [':protos'],",
        "  portable_proto_filters = ['data_filter.pbascii'],",
        ")");
    ConfiguredTarget topTarget = getObjcProtoAspectConfiguredTarget("//x:x");
    ObjcProtoProvider objcProtoProvider = topTarget.getProvider(ObjcProtoProvider.class);
    assertThat(objcProtoProvider).isNotNull();
  }

  @Test
  public void testObjcProtoAspectPropagatesProtobufProvider() throws Exception {
    scratch.file(
        "x/BUILD",
        "proto_library(",
        "  name = 'protos',",
        "  srcs = ['data.proto'],",
        ")",
        "objc_proto_library(",
        "  name = 'x',",
        "  deps = [':protos'],",
        "  portable_proto_filters = ['data_filter.pbascii'],",
        ")");
    ConfiguredTarget topTarget = getObjcProtoAspectConfiguredTarget("//x:x");
    ObjcProtoProvider objcProtoProvider = topTarget.getProvider(ObjcProtoProvider.class);
    assertThat(objcProtoProvider).isNotNull();
    assertThat(Artifact.toExecPaths(objcProtoProvider.getProtobufHeaders()))
        .containsExactly("objcproto/include/header.h");

    Artifact header = Iterables.getOnlyElement(objcProtoProvider.getProtobufHeaders());
    PathFragment includePath = header.getExecPath().getParentDirectory();
    PathFragment genIncludePath =
        PathFragment.create(
            getConfiguration(targetConfig, AppleCrosstoolTransition.APPLE_CROSSTOOL_TRANSITION)
                    .getGenfilesFragment()
                + "/"
                + includePath);

    assertThat(objcProtoProvider.getProtobufHeaderSearchPaths())
        .containsExactly(includePath, genIncludePath);
  }

  @Test
  public void testObjcProtoAspectDoesNotPropagateProviderWhenNoProtos() throws Exception {
    scratch.file("x/BUILD",
        "objc_library(",
        "  name = 'x',",
        "  srcs = ['A.m'],",
        ")");
    ConfiguredTarget topTarget = getObjcProtoAspectConfiguredTarget("//x:x");
    ObjcProtoProvider objcProtoProvider = topTarget.getProvider(ObjcProtoProvider.class);
    assertThat(objcProtoProvider).isNull();
  }

  @Test
  public void testObjcProtoAspectBundlesDuplicateSymbols() throws Exception {
    scratch.file(
        "x/BUILD",
        "objc_library(",
        "  name = 'x',",
        "  srcs = ['A.m'],",
        "  deps = [",
        "    ':objc_proto',",
        "    ':objc_proto_duplicate',",
        "  ],",
        ")",
        "proto_library(",
        "  name = 'protos',",
        "  srcs = ['data.proto'],",
        ")",
        "objc_proto_library(",
        "  name = 'objc_proto',",
        "  deps = [':protos'],",
        "  portable_proto_filters = ['data_filter.pbascii'],",
        ")",
        "objc_proto_library(",
        "  name = 'objc_proto_duplicate',",
        "  deps = [':protos'],",
        "  portable_proto_filters = ['data_filter.pbascii'],",
        ")");
    ConfiguredTarget topTarget = getObjcProtoAspectConfiguredTarget("//x:x");
    ObjcProtoProvider objcProtoProvider = topTarget.getProvider(ObjcProtoProvider.class);
    assertThat(objcProtoProvider).isNotNull();

    assertThat(Artifact.toExecPaths(Iterables.concat(objcProtoProvider.getProtoGroups())))
        .containsExactly("x/data.proto");
    assertThat(Artifact.toExecPaths(objcProtoProvider.getPortableProtoFilters()))
        .containsExactly("x/data_filter.pbascii");
  }

  @Test
  public void testObjcProtoAspectPropagatesGeneratedFilter() throws Exception {
    scratch.file(
        "x/BUILD",
        "objc_library(",
        "  name = 'x',",
        "  srcs = ['A.m'],",
        "  deps = [",
        "    ':objc_proto',",
        "  ],",
        ")",
        "proto_library(",
        "  name = 'protos',",
        "  srcs = ['data.proto'],",
        ")",
        "objc_proto_library(",
        "  name = 'objc_proto',",
        "  deps = [':protos'],",
        "  uses_protobuf = 1,",
        ")");
    ConfiguredTarget topTarget = getObjcProtoAspectConfiguredTarget("//x:x");
    ObjcProtoProvider objcProtoProvider = topTarget.getProvider(ObjcProtoProvider.class);
    assertThat(objcProtoProvider).isNotNull();

    assertThat(Artifact.toExecPaths(objcProtoProvider.getPortableProtoFilters()))
        .containsExactly(
            getConfiguration(targetConfig, AppleCrosstoolTransition.APPLE_CROSSTOOL_TRANSITION)
                    .getGenfilesFragment()
                + "/x/_proto_filters/objc_proto/generated_filter_file.pbascii");
  }

  @Test
  public void testObjcProtoAspectPropagatesFiltersFromDependenciesOfObjcProtoLibrary()
      throws Exception {
    scratch.file(
        "x/BUILD",
        "objc_library(",
        "  name = 'x',",
        "  srcs = ['A.m'],",
        "  deps = [",
        "    ':objc_proto_all',",
        "  ],",
        ")",
        "proto_library(",
        "  name = 'protos',",
        "  srcs = ['data.proto'],",
        ")",
        "objc_proto_library(",
        "  name = 'objc_proto_all',",
        "  deps = [':objc_proto_1', ':objc_proto_2'],",
        "  uses_protobuf = 1,",
        ")",
        "objc_proto_library(",
        "  name = 'objc_proto_1',",
        "  deps = [':protos'],",
        "  portable_proto_filters = ['filter.pbascii'],",
        ")",
        "objc_proto_library(",
        "  name = 'objc_proto_2',",
        "  deps = [':protos'],",
        "  uses_protobuf = 1,",
        ")");
    ConfiguredTarget topTarget = getObjcProtoAspectConfiguredTarget("//x:x");
    ObjcProtoProvider objcProtoProvider = topTarget.getProvider(ObjcProtoProvider.class);
    assertThat(objcProtoProvider).isNotNull();

    assertThat(Artifact.toExecPaths(objcProtoProvider.getPortableProtoFilters()))
        .containsAllOf(
            "x/filter.pbascii",
            getConfiguration(targetConfig, AppleCrosstoolTransition.APPLE_CROSSTOOL_TRANSITION)
                    .getGenfilesFragment()
                + "/x/_proto_filters/objc_proto_2/generated_filter_file.pbascii");
  }

  private ConfiguredTarget getObjcProtoAspectConfiguredTarget(String label) throws Exception {
    scratch.file("bin/BUILD",
        "objc_binary(",
        "  name = 'link_target',",
        "  deps = ['" + label + "'],",
        ")");

    return view.getPrerequisiteConfiguredTargetForTesting(
        reporter,
        getConfiguredTarget("//bin:link_target"),
        Label.parseAbsoluteUnchecked(label),
        masterConfig);
  }
}
