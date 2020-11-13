module fabcar

go 1.14

require (
	github.com/tw-bc-group/fabric-sdk-go-gm v1.0.0-beta3-gm
	github.com/Hyperledger-TWGC/tjfoc-gm v0.0.0-20201027032413-de75d571dd85
)

replace (
 github.com/tw-bc-group/fabric-sdk-go-gm => ./../../../fabric-sdk-go-gm
 github.com/Hyperledger-TWGC/tjfoc-gm v0.0.0-20201027032413-de75d571dd85 => ./../../../tjfoc-gm

)
