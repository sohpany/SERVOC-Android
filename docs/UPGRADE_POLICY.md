# HazRob Prototype Upgrade Policy

HazRob v0.3.0 is the prototype signing baseline. Test devices should uninstall pre-v0.3 builds once before installing v0.3.0 if Android reports a signature mismatch. From v0.3.0 onward, keep `applicationId` and the prototype signing certificate unchanged for iterative prototype upgrades.

Database migrations must remain non-destructive unless an explicit migration/export plan is documented and tested.
