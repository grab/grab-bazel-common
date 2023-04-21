package com.google.devtools.build.android;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceMerger {

    public static ParsedAndroidData emptyAndroidData() {
        return ParsedAndroidData.of(
                ImmutableSet.of(),
                ImmutableMap.of(),
                ImmutableMap.of(),
                ImmutableMap.of());
    }

    public static void merge(final List<SourceSet> sourceSets, final File outputDir) throws IOException {
        final Path target = Paths.get(outputDir.getAbsolutePath());
        Collections.reverse(sourceSets);
        final ImmutableList<DependencyAndroidData> deps = ImmutableList.copyOf(sourceSets
                .stream()
                .map(sourceSet -> new DependencyAndroidData(
                        /*resourceDirs*/ ImmutableList.copyOf(sourceSet.getResourceDirs()),
                        /*assetDirs*/ ImmutableList.copyOf(sourceSet.getAssetDirs()),
                        /*manifest*/ sourceSet.getManifest().toPath(),
                        /*rTxt*/ null,
                        /*symbols*/ null,
                        /*compiledSymbols*/ null
                )).collect(Collectors.toList()));

        final ParsedAndroidData androidData = ParsedAndroidData.from(deps);
        final AndroidDataMerger merger = AndroidDataMerger.createWithDefaults();

        final UnwrittenMergedAndroidData unwrittenMergedAndroidData = merger.doMerge(
                /*transitive*/ emptyAndroidData(),
                /*direct*/ emptyAndroidData(),
                /*parsedPrimary*/ androidData,
                /*primaryManifest*/ null,
                /*primaryOverrideAll*/ true,
                /*throwOnResourceConflict*/ false
        );
        final MergedAndroidData result = unwrittenMergedAndroidData.write(
                AndroidDataWriter.createWith(
                        /*manifestDirectory*/ target,
                        /*resourceDirectory*/ target.resolve("res"),
                        /*assertsDirectory*/ target.resolve("assets"),
                        /*executorService*/ MoreExecutors.newDirectExecutorService())
        );
    }
}
