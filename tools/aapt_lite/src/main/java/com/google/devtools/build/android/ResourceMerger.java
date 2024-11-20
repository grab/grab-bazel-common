package com.google.devtools.build.android;

import static com.android.manifmerger.ManifestMerger2.MergeType.APPLICATION;
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

import javax.annotation.Nullable;

public class ResourceMerger {
    private static final StdLogger STD_LOGGER = new StdLogger(StdLogger.Level.WARNING);

    public static ParsedAndroidData emptyAndroidData() {
        return ParsedAndroidData.of(
                ImmutableSet.of(),
                ImmutableMap.of(),
                ImmutableMap.of(),
                ImmutableMap.of());
    }

    public static void merge(final boolean isBinary,
                             final List<SourceSet> sourceSets,
                             @Nullable final File outputDir,
                             final File mergeManifest) throws IOException {
        mergeManifests(isBinary, sourceSets, mergeManifest);
        if (outputDir != null) {
            mergeResources(sourceSets, outputDir, mergeManifest);
        }
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

    /**
     * Do manifest merging just like Gradle would do for variant source sets i.e retain the placeholders in the Manifests and just do content
     * merging.
     *
     * @param isBinary       Whether the merging is for binary
     * @param sourceSets     The list of source sets
     * @param mergedManifest The result merged manifest file
     * @throws IOException Rethrow IO exceptions from manifest merger.
     */
    private static void mergeManifests(boolean isBinary,
                                       final List<SourceSet> sourceSets,
                                       final File mergedManifest) throws IOException {
        // https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:build-system/manifest-merger/src/test/java/com/android/manifmerger/ManifestMerger2SmallTest.java;l=792;drc=549798d9f7af50d4202041071bcb1f604e7229e9
        final AndroidManifestProcessor manifestProcessor = AndroidManifestProcessor.with(STD_LOGGER);

        final List<File> manifests = sourceSets
                .parallelStream()
                .map(SourceSet::getManifest)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Collections.reverse(manifests);

        if (manifests.size() > 1) {
            final File mainManifest = manifests.remove(0);
            try {
                final ManifestMerger2.MergeType mergeType = isBinary ? APPLICATION : LIBRARY;
                final MergingReport mergingReport = ManifestMerger2
                        .newMerger(mainManifest, STD_LOGGER, mergeType)
                        .withFeatures(Feature.NO_PLACEHOLDER_REPLACEMENT, Feature.EXTRACT_FQCNS)
                        .addFlavorAndBuildTypeManifests(manifests.toArray(File[]::new))
                        .merge();
                if (mergingReport.getResult().isError()) {
                    throw new IllegalStateException(mergingReport.getReportString());
                }
                manifestProcessor.writeMergedManifest(MERGED, mergingReport, mergedManifest.toPath());
            } catch (ManifestMerger2.MergeFailureException e) {
                throw new RuntimeException(e);
            }
        } else {
            final Optional<File> mainManifest = manifests.stream().findFirst();
            if (mainManifest.isPresent()) {
                Files.copy(mainManifest.get().toPath(), mergedManifest.toPath());
            } else {
                throw new IllegalArgumentException("Missing manifest declaration, check if at least one manifest is declared in any source set");
            }
        }
    }
}
