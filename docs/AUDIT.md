RevealActivity restores only the selected source URI after recreation and reloads the locked image instead of losing the selection or restoring controls without their model.
HideActivity excludes its view subtree from saved state so the restarted flow cannot restore controls from a discarded secret or enrollment session.
RevealActivity clears displayed plaintext, pending text animation, gesture state and door animation on destruction even when automatic locking is disabled.
RevealActivity replaces the stale exporting message with recovery guidance when a save picker is cancelled.
RevealExport removes orphaned decrypted temporary files on the first Reveal session after process death and retries cleanup before creating another export.
HideStore owns and recycles cover thumbnails on replacement and closure; HideActivity detaches the preview before worker cleanup, including discarded callbacks.
CoverStep and ScannerActivity defer prepared camera launches until their host resumes instead of launching from a paused activity.
ScannerActivity invalidates previous results and hides the old Reveal action before rejecting missing or unsupported incoming URIs.
ScannerActivity prevents duplicate camera launches, retains pending capture state across recreation, and removes cancelled, replaced and abandoned capture files.
ScannerActivity skips obsolete queued decodes before allocating large pixel arrays and drains pending capture cleanup when closing.
ImageDecoder uses bounded streaming PNG chunk and CRC validation to reject truncated chunks, missing termination and checksum damage before decoding partial pixels.
ImageDecoder recycles allocated bitmaps when opacity inspection or rendering fails with an allocation error.
DiffMapData recycles its X-ray bitmap as well as its difference bitmap if subsequent statistics allocation fails.
SyncManager reads airplane-mode settings on its worker and uses the broadcast state directly instead of querying the settings provider on the UI thread.
ResultStep exposes the existing comparison proof-sheet export on Android 8 and 9, where it was incorrectly omitted with the unavailable gallery action.
The Hide back control has an explicit accessible Back description.
ResultStep obtains its suggested PNG filename from strings.xml instead of hard-coding picker text.
