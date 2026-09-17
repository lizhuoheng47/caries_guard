# Clinical core

Transitional service boundary containing patient, visit, case, identity, and organization workflows currently hosted by `backend-java/caries-boot`. It remains the compatibility facade while downstream capabilities are extracted.

New extracted services must access clinical identifiers and authorized metadata through versioned APIs or events, never by querying clinical tables.
