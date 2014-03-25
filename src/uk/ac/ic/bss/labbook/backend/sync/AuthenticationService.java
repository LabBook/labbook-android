package uk.ac.ic.bss.labbook.backend.sync;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class AuthenticationService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return new AbstractAccountAuthenticator(this) {

			@Override
			public Bundle updateCredentials(AccountAuthenticatorResponse response,
					Account account, String authTokenType, Bundle options)
							throws NetworkErrorException {
				System.out.println("@@@AuthenticationService.updateCredentials");
				return null;
			}

			@Override
			public Bundle hasFeatures(AccountAuthenticatorResponse response,
					Account account, String[] features) throws NetworkErrorException {
				System.out.println("@@@AuthenticationService.hasFeatures");
				return null;
			}

			@Override
			public String getAuthTokenLabel(String authTokenType) {
				System.out.println("@@@AuthenticationService.getAuthTokenLabel");
				return null;
			}

			@Override
			public Bundle getAuthToken(AccountAuthenticatorResponse response,
					Account account, String authTokenType, Bundle options)
							throws NetworkErrorException {
				System.out.println("@@@AuthenticationService.getAuthToken");
				return null;
			}

			@Override
			public Bundle editProperties(AccountAuthenticatorResponse response,
					String accountType) {
				System.out.println("@@@AuthenticationService.editProperties");
				return null;
			}

			@Override
			public Bundle confirmCredentials(AccountAuthenticatorResponse response,
					Account account, Bundle options) throws NetworkErrorException {
				System.out.println("@@@AuthenticationService.confirmCredentials");
				return null;
			}

			@Override
			public Bundle addAccount(AccountAuthenticatorResponse response,
					String accountType, String authTokenType,
					String[] requiredFeatures, Bundle options)
							throws NetworkErrorException {
				System.out.println("@@@AuthenticationService.addAccount");
				return null;
			}
		}.getIBinder();
	}

}
