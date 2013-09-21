sw-backend
==========

TODO: Make sure that no unencrypted passwords are passed between servers. It's currently the case in the *ThirdPartyEmailSender* app, where the app fetches unencrypted passwords from the platform DB. The *ThirdPartyEmailSender* app resides on another PC than the platform DB.
