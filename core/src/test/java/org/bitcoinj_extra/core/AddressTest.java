/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj_extra.core;

import org.bitcoinj_extra.params.MainNetParams;
import org.bitcoinj_extra.params.Networks;
import org.bitcoinj_extra.params.TestNet3Params;
import org.bitcoinj_extra.script.Script;
import org.bitcoinj_extra.script.ScriptBuilder;
import org.bitcoinj_extra.shapeshift.ShapeShift;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import static org.bitcoinj_extra.core.Utils.HEX;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class AddressTest {
    static final NetworkParameters testParams = TestNet3Params.get();
    static final NetworkParameters mainParams = MainNetParams.get();

    @Test
    public void testJavaSerialization() throws Exception {
        Address testAddress = Address.fromBase58(testParams, "n4eA2nbYqErp7H6jebchxAN59DmNpksexv");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(testAddress);
        VersionedChecksummedBytes testAddressCopy = (VersionedChecksummedBytes) new ObjectInputStream(
                new ByteArrayInputStream(os.toByteArray())).readObject();
        assertEquals(testAddress, testAddressCopy);

        Address mainAddress = Address.fromBase58(mainParams, "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");
        os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(mainAddress);
        VersionedChecksummedBytes mainAddressCopy = (VersionedChecksummedBytes) new ObjectInputStream(
                new ByteArrayInputStream(os.toByteArray())).readObject();
        assertEquals(mainAddress, mainAddressCopy);
    }

    @Test
    public void stringification() throws Exception {
        // Test a testnet address.
        Address a = new Address(testParams, HEX.decode("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"));
        assertEquals("n4eA2nbYqErp7H6jebchxAN59DmNpksexv", a.toString());
        assertFalse(a.isSelectedP2SHAddress());

        Address b = new Address(mainParams, HEX.decode("4a22c3c4cbb31e4d03b15550636762bda0baf85a"));
        assertEquals("17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL", b.toString());
        assertFalse(b.isSelectedP2SHAddress());
    }
    
    @Test
    public void decoding() throws Exception {
        Address a = Address.fromBase58(testParams, "n4eA2nbYqErp7H6jebchxAN59DmNpksexv");
        assertEquals("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc", Utils.HEX.encode(a.getHash160()));

        Address b = Address.fromBase58(mainParams, "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");
        assertEquals("4a22c3c4cbb31e4d03b15550636762bda0baf85a", Utils.HEX.encode(b.getHash160()));
    }
    
    @Test
    public void errorPaths() {
        // Check what happens if we try and decode garbage.
        try {
            Address.fromBase58(testParams, "this is not a valid address!");
            fail();
        } catch (WrongNetworkException e) {
            fail();
        } catch (AddressFormatException e) {
            // Success.
        }

        // Check the empty case.
        try {
            Address.fromBase58(testParams, "");
            fail();
        } catch (WrongNetworkException e) {
            fail();
        } catch (AddressFormatException e) {
            // Success.
        }

        // Check the case of a mismatched network.
        try {
            Address.fromBase58(testParams, "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");
            fail();
        } catch (WrongNetworkException e) {
            // Success.
            assertEquals(e.verCode, MainNetParams.get().getAddressHeader());
            assertTrue(Arrays.equals(e.acceptableVersions, TestNet3Params.get().getAcceptableAddressCodes()));
        } catch (AddressFormatException e) {
            fail();
        }
    }

    @Test
    public void getNetwork() throws Exception {

        Networks.register(TestNet3Params.get());

        CoinDetails details = Address.getCoinsFromAddress("17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL").get(0);
        assertEquals(MainNetParams.get().getId(), details.getId());
        details = Address.getCoinsFromAddress("n4eA2nbYqErp7H6jebchxAN59DmNpksexv").get(0);
        assertEquals(TestNet3Params.get().getId(), details.getId());
        
    }

    @Test
    public void getAltNetwork() throws Exception {
        // An alternative network
        class AltNetwork extends MainNetParams {
            AltNetwork() {
                super();
                id = "alt.network";
                addressHeader = 48;
                p2shHeader = 5;
                acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
            }
        }
        AltNetwork altNetwork = new AltNetwork();
        // Add new network params
        Networks.register(altNetwork);
        // Check if can parse address
        CoinDetails details = Address.getCoinsFromAddress("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6").get(0);
        assertEquals(altNetwork.getId(), details.getId());
        // Check if main network works as before
        details = Address.getCoinsFromAddress("17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL").get(0);
        assertEquals(MainNetParams.get().getId(), details.getId());
        // Unregister network
        Networks.unregister(altNetwork);
        try {
            Address.getCoinsFromAddress("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6");
            fail();
        } catch (AddressFormatException e) { }
    }

    @Test
    public void shapeshiftAddresses() {

        // Need to add coins first
        ShapeShift.addAllCoins();

        // Bitcoin pubkey address, all coins

        List<CoinDetails> details = Address.getCoinsFromAddress("17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");

        assertEquals(6, details.size());

        assertEquals("bitcoin", details.get(0).getUriScheme());
        assertEquals("counterparty_bcy", details.get(1).getUriScheme());
        assertEquals("counterparty_xcp", details.get(2).getUriScheme());
        assertEquals("counterparty_maid", details.get(3).getUriScheme());
        assertEquals("mastercoin", details.get(4).getUriScheme());
        assertEquals("counterparty_sjcx", details.get(5).getUriScheme());

        // Bitcoin P2SH address, all coins

        details = Address.getCoinsFromAddress("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU");

        assertEquals(12, details.size());

        assertEquals("bitcoin", details.get(0).getUriScheme());
        assertEquals("counterparty_bcy", details.get(1).getUriScheme());
        assertEquals("counterparty_xcp", details.get(2).getUriScheme());
        assertEquals("digibyte", details.get(3).getUriScheme());
        assertEquals("litecoin", details.get(4).getUriScheme());
        assertEquals("counterparty_maid", details.get(5).getUriScheme());
        assertEquals("mastercoin", details.get(6).getUriScheme());
        assertEquals("monacoin", details.get(7).getUriScheme());
        assertEquals("reddcoin", details.get(8).getUriScheme());
        assertEquals("startcoin", details.get(9).getUriScheme());
        assertEquals("counterparty_sjcx", details.get(10).getUriScheme());
        assertEquals("vertcoin", details.get(11).getUriScheme());

        // Peercoin example

        details = Address.getCoinsFromAddress("PR67EtpptSrCBvAW3jei7rg7472A7bYGsK");
        assertEquals(1, details.size());
        assertEquals("ppcoin", details.get(0).getId());

        // TODO: More Shapeshift tests!

    }
    
    @Test
    public void p2shAddress() throws Exception {
        // Test that we can construct P2SH addresses
        Address mainNetP2SHAddress = Address.fromBase58(MainNetParams.get(), "35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU");
        assertEquals(mainNetP2SHAddress.version, MainNetParams.get().p2shHeader);
        assertTrue(mainNetP2SHAddress.isSelectedP2SHAddress());
        Address testNetP2SHAddress = Address.fromBase58(TestNet3Params.get(), "2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe");
        assertEquals(testNetP2SHAddress.version, TestNet3Params.get().p2shHeader);
        assertTrue(testNetP2SHAddress.isSelectedP2SHAddress());

        // Need TestNet3Params for next test
        Networks.register(TestNet3Params.get());

        // Test that we can determine what network a P2SH address belongs to
        CoinDetails mainNetParams = Address.getCoinsFromAddress("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU").get(0);
        assertEquals(MainNetParams.get().getId(), mainNetParams.getId());
        CoinDetails testNetParams = Address.getCoinsFromAddress("2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe").get(0);
        assertEquals(TestNet3Params.get().getId(), testNetParams.getId());

        // Test that we can convert them from hashes
        byte[] hex = HEX.decode("2ac4b0b501117cc8119c5797b519538d4942e90e");
        Address a = Address.fromP2SHHash(mainParams, hex);
        assertEquals("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU", a.toString());
        Address b = Address.fromP2SHHash(testParams, HEX.decode("18a0e827269b5211eb51a4af1b2fa69333efa722"));
        assertEquals("2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe", b.toString());
        Address c = Address.fromP2SHScript(mainParams, ScriptBuilder.createP2SHOutputScript(hex));
        assertEquals("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU", c.toString());
    }

    @Test
    public void p2shAddressCreationFromKeys() throws Exception {
        // import some keys from this example: https://gist.github.com/gavinandresen/3966071
        ECKey key1 = DumpedPrivateKey.fromBase58(mainParams, "5JaTXbAUmfPYZFRwrYaALK48fN6sFJp4rHqq2QSXs8ucfpE4yQU").getKey();
        key1 = ECKey.fromPrivate(key1.getPrivKeyBytes());
        ECKey key2 = DumpedPrivateKey.fromBase58(mainParams, "5Jb7fCeh1Wtm4yBBg3q3XbT6B525i17kVhy3vMC9AqfR6FH2qGk").getKey();
        key2 = ECKey.fromPrivate(key2.getPrivKeyBytes());
        ECKey key3 = DumpedPrivateKey.fromBase58(mainParams, "5JFjmGo5Fww9p8gvx48qBYDJNAzR9pmH5S389axMtDyPT8ddqmw").getKey();
        key3 = ECKey.fromPrivate(key3.getPrivKeyBytes());

        List<ECKey> keys = Arrays.asList(key1, key2, key3);
        Script p2shScript = ScriptBuilder.createP2SHOutputScript(2, keys);
        Address address = Address.fromP2SHScript(mainParams, p2shScript);
        assertEquals("3N25saC4dT24RphDAwLtD8LUN4E2gZPJke", address.toString());
    }

    @Test
    public void cloning() throws Exception {
        Address a = new Address(testParams, HEX.decode("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"));
        Address b = a.clone();

        assertEquals(a, b);
        assertNotSame(a, b);
    }

    @Test
    public void roundtripBase58() throws Exception {
        String base58 = "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL";
        assertEquals(base58, Address.fromBase58(null, base58).toBase58());
    }

    @Test
    public void comparisonCloneEqualTo() throws Exception {
        Address a = Address.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX");
        Address b = a.clone();

        int result = a.compareTo(b);
        assertEquals(0, result);
    }

    @Test
    public void comparisonEqualTo() throws Exception {
        Address a = Address.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX");
        Address b = a.clone();

        int result = a.compareTo(b);
        assertEquals(0, result);
    }

    @Test
    public void comparisonLessThan() throws Exception {
        Address a = Address.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX");
        Address b = Address.fromBase58(mainParams, "1EXoDusjGwvnjZUyKkxZ4UHEf77z6A5S4P");

        int result = a.compareTo(b);
        assertTrue(result < 0);
    }

    @Test
    public void comparisonGreaterThan() throws Exception {
        Address a = Address.fromBase58(mainParams, "1EXoDusjGwvnjZUyKkxZ4UHEf77z6A5S4P");
        Address b = Address.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX");

        int result = a.compareTo(b);
        assertTrue(result > 0);
    }

    @Test
    public void comparisonBytesVsString() throws Exception {
        // TODO: To properly test this we need a much larger data set
        Address a = Address.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX");
        Address b = Address.fromBase58(mainParams, "1EXoDusjGwvnjZUyKkxZ4UHEf77z6A5S4P");

        int resultBytes = a.compareTo(b);
        int resultsString = a.toString().compareTo(b.toString());
        assertTrue( resultBytes < 0 );
        assertTrue( resultsString < 0 );
    }
}
