module fabcar

go 1.14

require (
	github.com/Hyperledger-TWGC/tjfoc-gm v0.0.0-20201027032413-de75d571dd85 // indirect
	github.com/tw-bc-group/fabric-sdk-go-gm v1.0.0-beta3-gm
)

replace (
	github.com/Hyperledger-TWGC/tjfoc-gm v0.0.0-20201027032413-de75d571dd85 => ./../../../tjfoc-gm
	github.com/tw-bc-group/fabric-sdk-go-gm => ./../../../fabric-sdk-go-gm

)
