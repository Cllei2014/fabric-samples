.PHONY: fabcar
fabcar:
	@echo Start the chain with Fabcar
	./scripts/ci_scripts/test_fabcar.sh ./startFabric.sh

.PHONY: fabcar-stop
fabcar-clean:
	@echo Clean all with Fabcar
	./scripts/ci_scripts/test_fabcar.sh ./stopFabric.sh

.PHONY: sdk-test
sdk-test:
	./scripts/ci_scripts/test_sdk.sh ./runSDK.sh

