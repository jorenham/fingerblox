# Meeting Notes 8/3/2017

KEEP USING CAMERA (more flashy-catchy!!):

* Make a universal app for all phones where you just
change settings depending on your phone
* Use of SHIFT for finger detection
* Use white background for mackground contrast
* Specific distanse from camera

*Steps by Pouwelse:*

Task for final project prototype:

Android app
* Reach the WOW factor: it simply works!
* Android app only (no special fingerprint scanner hardware needed)
* Step 1: within the app: record a white background
* Step 2: move your finger in the recording box
* Step 3: make picture recording
* [Sift for fingerprint feature extraction] (http://opencv-python-tutroals.readthedocs.io/en/latest/py_tutorials/py_feature2d/py_sift_intro/py_sift_intro.html)
* [Fingerprint Identification Using SIFT-Based Minutia Descriptors and Improved All Descriptor-Pair Matching] (https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3658737/)
* [This Polish algorithm] (https://github.com/rtshadow/biometrics) does skeletonization
* [Now the critical step of Minutiae recognition (crossing number method)] (https://github.com/rtshadow/biometrics#minutiae-recognition-crossing-number-method) within the Polish algorithm
* Integrate OpenCV inside Android app
* Best to do all processing within OpenCV if possible and not use the Polish Python code.
* Delft Blockchain code in Python can be ignored for now, possibly link that within the final week of project.
* Goal is operational app: OK to hardcode for 1 Android phone model camera (Nexus, S6, G5, etc.)
For instance, require autofocus. Set fixed aperture lens from within the app.
* Ignore for now the dataset issue. No attention coming sprint on matching fingerprints with dataset, training, and testing. Just skeleton app sprint.
