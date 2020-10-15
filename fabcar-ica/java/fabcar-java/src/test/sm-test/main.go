package main

import (
	"encoding/asn1"
	"encoding/base64"
	_ "fmt"
	"github.com/tjfoc/gmsm/sm2"
	"log"
	"math/big"
	"os"
)

var (
	default_uid = []byte{0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38}
)

type sm2Signature struct {
	R, S *big.Int
}

func Sign(body string) (string, error) {
	cwd, _ := os.Getwd()
	PriKeyPath := cwd + string(os.PathSeparator) + "sm2PriKeyPkcs8.pem"

	priKey, e := sm2.ReadPrivateKeyFromPem(PriKeyPath, nil)
	if e != nil {
		log.Println("priKeyPem read failed, error: ", e)
		return "", e
	}

	r, s, err := sm2.Sm2Sign(priKey, []byte(body), default_uid)
	//r, s, err := sm2.Sign(priKey, []byte(body))

	if err != nil {
		log.Println("priKey sign error: ", err)
		return "", err
	}

	log.Println("priKey signature r: %s", base64.StdEncoding.EncodeToString(r.Bytes()))
	log.Println("priKey signature s: %s", base64.StdEncoding.EncodeToString(s.Bytes()))

	marshal, _ := asn1.Marshal(sm2Signature{r, s})
	signature := base64.StdEncoding.EncodeToString(marshal)

	//c := priKey.PublicKey.Curve
	//N := c.Params().N

	//Buffer是一个实现了读写方法的可变大小的字节缓冲
	//var buffer bytes.Buffer
	//// TODO: add N by me
	////buffer.Write(N.Bytes())
	//buffer.Write(r.Bytes())
	//buffer.Write(s.Bytes())
	////
	//signature := base64.StdEncoding.EncodeToString(buffer.Bytes())

	log.Println("priKey signature base64: ", signature)
	return signature, nil
}

func Verify(body, signature string) {
	cwd, _ := os.Getwd()
	PubKeyPath := cwd + string(os.PathSeparator) + "sm2PubKey.pem"

	pubKey, e := sm2.ReadPublicKeyFromPem(PubKeyPath, nil)

	if e != nil {
		log.Println("pubKeyPem read failed, error: ", e)
	}

	d64, err := base64.StdEncoding.DecodeString(signature)
	if err != nil {
		log.Println("base64 decode error: ", err)
	}

	l := len(d64)
	br := d64[:l/2]
	bs := d64[l/2:]

	var ri, si big.Int
	r := ri.SetBytes(br)
	s := si.SetBytes(bs)
	v := sm2.Sm2Verify(pubKey, []byte(body), default_uid, r, s)
	log.Printf("pubKey verified: %v\n", v)
}

func main() {
	//body := `{"name":"mike","gender":"male"}`
	body := `yin`
	//signature := `MEUCIB9V3X2GmbJ4FqpSVVKbkbFz+S4cCU9ZAiTD6K2BwvtwAiEAvmLTh3Qv1a8uj5zrEJWDEdeIXRq2cKbfh+LxjwWQhOM=`
	signature, _ := Sign(body)
	Verify(body, signature)
}
