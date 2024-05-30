package com.google.devtools.build.android;

import static com.android.manifmerger.ManifestMerger2.MergeType.LIBRARY;
import static com.android.manifmerger.MergingReport.MergedManifestKind.MERGED;

import com.android.manifmerger.ManifestMerger2;
import com.android.manifmerger.ManifestMerger2.Invoker.Feature;
import com.android.manifmerger.MergingReport;
import com.android.utils.StdLogger;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResourceMerger {
    private static final StdLogger STD_LOGGER = new StdLogger(StdLogger.Level.WARNING);

    public static ParsedAndroidData emptyAndroidData() {
        return ParsedAndroidData.of(
                ImmutableSet.of(),
                ImmutableMap.of(),
                ImmutableMap.of(),
                ImmutableMap.of());
    }

    public static void merge(final List<SourceSet> sourceSets, final File outputDir, final File manifest) throws IOException {
        mergeManifests(sourceSets, manifest);
        mergeResources(sourceSets, outputDir, manifest);
    }

    private static void mergeResources(final List<SourceSet> sourceSets, final File outputDir, final File manifest) throws IOException {
        final Path target = Paths.get(outputDir.getAbsolutePath());
        Collections.reverse(sourceSets);
        final ImmutableList<DependencyAndroidData> deps = ImmutableList.copyOf(sourceSets
                .stream()
                .map(sourceSet -> new DependencyAndroidData(
                        /*resourceDirs*/ ImmutableList.copyOf(sourceSet.getResourceDirs()),
                        /*assetDirs*/ ImmutableList.copyOf(sourceSet.getAssetDirs()),
                        /*manifest*/ sourceSet.getManifest() != null ? sourceSet.getManifest().toPath() : manifest.toPath(),
                        /*rTxt*/ null,
                        /*symbols*/ null,
                        /*compiledSymbols*/ null
                )).collect(Collectors.toList()));

        final ParsedAndroidData androidData = ParsedAndroidData.from(deps);
        final AndroidDataMerger androidDataMerger = AndroidDataMerger.createWithDefaults();

        final UnwrittenMergedAndroidData unwrittenMergedAndroidData = androidDataMerger.doMerge(
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

    private static void mergeManifests(List<SourceSet> sourceSets, File manifest) throws IOException {
        // https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:build-system/manifest-merger/src/test/java/com/android/manifmerger/ManifestMerger2SmallTest.java;l=792;drc=549798d9f7af50d4202041071bcb1f604e7229e9
        final List<File> manifests = sourceSets
                .stream()
                .map(SourceSet::getManifest)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (manifests.size() > 1) {
            final File primary = manifests.remove(0);
            try {
                final MergingReport mergingReport = ManifestMerger2
                        .newMerger(primary, STD_LOGGER, LIBRARY)
                        .withFeatures(Feature.NO_PLACEHOLDER_REPLACEMENT)
                        .addFlavorAndBuildTypeManifests(manifests.toArray(File[]::new))
                        .merge();
                AndroidManifestProcessor
                        .with(STD_LOGGER)
                        .writeMergedManifest(MERGED, mergingReport, manifest.toPath());
            } catch (ManifestMerger2.MergeFailureException e) {
                throw new RuntimeException(e);
            }
        } else {
            final Optional<File> primaryManifest = manifests.stream().findFirst();
            if (primaryManifest.isPresent()) {
                Files.copy(primaryManifest.get().toPath(), manifest.toPath());
            } else {
                throw new IllegalArgumentException("Missing manifest declaration, check if at least one manifest is declared in any source set");
            }
        }
    }
}
