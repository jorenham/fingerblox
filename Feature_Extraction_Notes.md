# Fingerprint Recognition

## Feature Extraction through camera lenses

### Steps for fingerprint feature extraction and fingerprint matching
* Create a camera overlay for correct finger placement in front of the lenses
* User takes several photos (number of photos to be determined) of fingerprint
* Photos are preprocessed
* Predetermined set of features is extracted from the photos
* Fingerprint features are sent to the database
    * If user is creating a new profile then fingerprint data are stored in database
    * If user is already registed, fingerprint data are compared with others in the database
        * If match is found the user is authenticated
        * If match is not found then the user has to repeat the process or is not granted an access

## Fingerprint Matching
Each user could have a registered fingerprint profile where the input fingerprint features (from camera) are compared with the user's profile fingerprint features. The compare could be done by a classifier 