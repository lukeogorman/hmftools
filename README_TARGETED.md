# Targeted NGS Analysis in HMF Tools

Whilst designed initiallly for WGS, the core HMF tools have been adapted to fully support targeted sequenincing input.   The implementation is panel independent, but each new panel requires an initial set of input samples (20-50) for training to learn the read depth profile (see 'Generation of targetRegions CN normalisation file' section below) as well as a target bed file to identify the targeted regions for that panel.   To estimate MSI, a set of microsatellites with high coverage in the panel must also be defined.

The key changes in targeted mode are mostly with 2 components
• Cobalt normalises copy number and masks off target regions according to the CN normalisation file
• PURPLE has custom routines for TMB / TML / MSI and special rules for calling drivers

Other components, operate essentially the same but may also require different configuration to reflect ths sparsity of data, higher on target depth and .   We have so far implemented 2 broad panels only: TSO500 (1.3Mb) & HMF panel (2Mb).   The configuration suggested below should work well for these panels with on target depth of ~300-2000x

A demo of the targeted pipeline is available [here](https://github.com/hartwigmedical/hmftools/blob/master/pipeline/README_PANEL.md)

## Special resources files for targeted mode

The following files are all required in targeted mode and are panel specific:

| File Name                      | Tool   | Purpose                                                    |
|--------------------------------|--------|------------------------------------------------------------|
| targetRegionsNormalisation.tsv | Cobalt | Normalise copy number regions and mask off target regions. |
| CodingRegions.bed              | Purple | Coding regions to consider for TMB/TML model.              |
| MSI.Bed                        | Purple | List of MSI locii to consider in MSI model.                |

The driverGenePanel.tsv, ActionableCodingPanel.bed and CoverageCodingPanel.bed should also all be adapted to match the specific panel

## Recommended configuration changes for broad panel (500k-2Mb) with 200-2000x depth

AMBER
```
-tumor_only_min_depth 80
```
COBALT
```
-target_region {targetRegionsNormalisation}
-pcf_gamma 15
```
SAGE
```
-hotspot_min_tumor_vaf 0.005
-hotspot_min_tumor_qual 100
-panel_min_tumor_qual 250
-high_confidence_min_tumor_qual 350
-low_confidence_min_tumor_qual 500
-max_read_depth 100000
-sync_fragments=True
```
GRIPSS
```
-hard_min_tumor_qual 200 
-min_qual_break_point 1000
-min_qual_break_end 1000
-target_regions_bed= Target Regions BED file
```
PURPLE
```
-min_diploid_tumor_ratio_count 3
-min_diploid_tumor_ratio_count_centromere 3
-target_regions_bed ${target_regions_definition}
-target_regions_ratios ${target_regions_ratios}
-target_regions_msi_indels ${target_regions_msi_indels}
-ploidy_penalty_standard_derviation = 0.15
-ploid_penalty_factor = 0.6
```

## Targeted specific methods
### Cobalt target regions normalisation file creation

A cobalt normalisation file for an arbitary panel can becreated using a set of training samples (recommended at least 20 samples) to learn the copy number biases in on-target regions.  The procedrue for doing this is as follows:

1. Run Cobalt on the training samples without in tumor-only mode WITHOUT a '-target_region' file specified

2a. Run Amber on applicable samples in tumor-only mode
- set '-tumor_only_min_depth 2' to ensure sufficient read coverage over heterozygous points
OR
2b. Set Amber gender in for each applicable sample in sample ID file in step 3

3. Run the Cobalt normalisation file builder command described below.  This performs the following steps
- for each 1K region covering any target region, extract each sample's tumor read count and the GC profile mappability and GC ratio bucket
- calculate median and median read counts for each sample, and overall sample mean and median counts
- normalise each sample's tumor read counts per region
- calculate a median read count from all samples per GC ratio bucket  
- write a relative enrichment for each region to the output file, with a min enrichment of 0.1
- if no WGS is available for normalisation, the tumorGCRatio is assumed to be 1 for autosomes. The gender of each sample must be provided. Female samples are excluded from Y chromosome normalisation and males use a tumorGCRatio of 0.5 for the sex chromosomes

The output of this process is a targetRegionsNormalisation file with the expected relative enrichment for each on target region.

#### Arguments

Field | Description
---|---
sample_id_file | CSV with SampleId column header and list of sample IDs, optionally Gender column if Amber directory is not specified & available
amber_dir | Pipeline Amber output directory
cobalt_dir | Pipeline Cobalt output directory from non-normalised run on panel BAMs
ref_genome_version | V37 or V38
gc_profile | As used in Cobalt and Purple
target_regions_bed | Definition of target regions
output_file | Output normalisation TSV file

#### Command

```
java -cp cobalt.jar com.hartwig.hmftools.cobalt.norm.NormalisationFileBuilder 
  -sample_id_file sample_ids.csv
  -cobalt_dir /path_to_pipeline_cobalt_data/
  -amber_dir /path_to_pipeline_amber_data/ 
  -ref_genome_version V37 
  -gc_profile /ref_data/GC_profile.1000bp.37.cnp 
  -target_regions_bed /ref_data/target_regions_definition.37.bed 
  -output_file /data/output/target_regions.cobalt_normalisation.37.tsv 
  -log_debug
```

### COBALT behaviour in targeted mode
If a targetRegions file is provided, then a target enrichment rate is calculated simply as the median tumorGCRatio for the specified regions.   Any depth windows outside of the targetRegions file are masked so that they are ignored downstream by PURPLE. Depth windows found in the TSV file are normalised first by the overall target enrichment rate for the sample, then by the relativeEnrichment for that depth window and finally by the normal GC bias adjustment.   The GC bias is calculated using on target regions only.

### PURPLE MSI 

For a set of microsatellite sites defined in the MSI target bed file count the number of passing variants at MSI sites ignoring SNV, MNV and 1 base deletes and requiring a VAF cutoff of > 0.15 for 2 and 3 base deletes or 0.08 for 4+ base deletes or any length insertion.

We estimate MSI rate as:
```
MSIndelsPerMb = 220 * # of MSI variants / # of MSI sites in panel
```
### PURPLE TML & TMB estimate

A custom model is used for TMB estimated in targeted mode. The main challenges of the model is to determine variants are included in the TMB estimate. PURPLE selects variants that meet the following criteria:
- Coding effect <> NONE
- GNDFreq <0.00005
- GENE in PANEL and not in {HLA-A,HLA-B,HLA-C,PIM1,BCL2} 
- Type = SNV
- !HOTSPOT
- AF < 0.9

Each variant included is classified as ‘somatic’ if somatic likelihood = HIGH.    If somatic likelihood = MEDIUM, then the variant is marked as 'unclear'.

The final somatic count estimate is set to = somatic + unclear^2 / ( CodingBases/170,000 + unclear).

This function is intended to reflect that when the number of unclear variants is less than expected germline variants then most unclear variants will be germline, whereas where the number of unclear variants is very high most will be somatic.

Using this number we then estimate the mutational burden as follows
```
TML = somatic Variant Estimate / CodingBases * RefGenomeCodingBases 
TMB = 0.05 * TML + MSIIndelPerMb
```
The 0.05 conversion from TML to TMB is the empirically observed relationship in the Hartwig database.

For driver likelihood calculations, we assume 20% of variants are biallelic for targeted sequencing samples.

### Other PURPLE differences in targeted mode

The following special rules apply to the consrtuction of the driver catalog
- **DELS**: Don’t report DELS >10Mb or if the copy number segment has less than 3 depth windows (unless supported by SV on both sides)
- **PARTIAL_AMP**: only in genes with known pathogenic exon deletions {BRAF, EGFR, CTNNB1, CBL,MET, ALK, PDGFRA}

There is also no somatic fit mode or somatic penalty and no SV recovery in PURPLE in targeted mode.

## Panel specific PONs	

We constructed a panel specific PON separately for each panel based on the 48 HMF and XX TSO500 samples we have analysed. Any non hotspot variant found 3 or more times with a qual of > 100 was excluded.

We also added the following HOTSPOT variants to both PONs:
chr3:142555897:AT>A
chrX:67545400:GGCA>G

Variants that were already in our normal PON were excluded.

## Future improvements

- **Overlapping reads** - Overlapping reads in the same fragment are common in FFPE Panel libraries. We should ideally only use the highest qual at each base in case of overlap.
- **UMI** - We should switch to using UMI to better filter duplicates
- **Off Target normalisation and integration** - This is implemented, but not used as currently does not yield a benefit over on target alone.
MSI thresholds - We could better estimate if we had a more diverse range of samples for testing with known MSIndelsPerMb rates around and above the MSI cutoff.
- **HRD prediction** - We can likely train a custom model, but we don’t have enough known HRD+ samples with panel data at the moment to evaluate.
- **Purity & ploidy estimates** - Purity and ploidy estimates are only correct approximately half the time.   The fit could be made more robust by improving -COBALT/AMBER parameterisation, merging on and off target regions or changing PURPLE fit functionality
