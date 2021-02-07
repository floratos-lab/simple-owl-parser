# simple-owl-parser
This is beyond a simple parser for VO.owl now.

It is the 'informal' code to prepare background data for HIPC dashboard.

* `CellSubsetDataTool.java` creates `cellsubset-list.txt`, which is meant to be copied to hipc dashboard project, `admin` module. It requires further manual editing there (adding a few terms).
* `VaccineDataTool.java` creates `simple-vaccine-list.txt`, the vaccine backgrounddata for hipc. It should be copied to hipc dashboard project.
* `PathogenDataTool.java` creates `pathogen-list.txt`. It should be copied to `subject_data/pathogen` directory of HIPC dashboard code's data directory.