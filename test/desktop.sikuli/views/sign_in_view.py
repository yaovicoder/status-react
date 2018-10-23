import os

from _pytest.runner import Failed

from views.base_element import BaseElement, InputField
from views.base_view import BaseView
from views.home_view import HomeView

IMAGES_PATH = os.path.join(os.path.dirname(__file__), 'images/sign_in_view')


class CreateAccountButton(BaseElement):
    def __init__(self):
        super(CreateAccountButton, self).__init__(IMAGES_PATH + '/create_account.png')

    def click(self):
        try:
            super(CreateAccountButton, self).click()
        except Failed:
            self.screenshot = IMAGES_PATH + '/create_new_account.png'
            super(CreateAccountButton, self).click()


class IHaveAccountButton(BaseElement):
    def __init__(self):
        super(IHaveAccountButton, self).__init__(IMAGES_PATH + '/i_have_account.png')


class OtherAccountsButton(BaseElement):
    def __init__(self):
        super(OtherAccountsButton, self).__init__(IMAGES_PATH + '/other_accounts.png')


class PrivacyPolicyButton(BaseElement):
    def __init__(self):
        super(PrivacyPolicyButton, self).__init__(IMAGES_PATH + 'privacy_policy_button.png')


class CreatePasswordInput(InputField):
    def __init__(self):
        super(CreatePasswordInput, self).__init__(IMAGES_PATH + '/create_password_input.png')


class ConfirmPasswordInput(InputField):
    def __init__(self):
        super(ConfirmPasswordInput, self).__init__(IMAGES_PATH + '/confirm_password_input.png')


class UserNameInput(InputField):
    def __init__(self):
        super(UserNameInput, self).__init__(IMAGES_PATH + '/username_input.png')


class NextButton(BaseElement):
    def __init__(self):
        super(NextButton, self).__init__(IMAGES_PATH + '/next_button.png')


class RecoveryPhraseInput(InputField):
    def __init__(self):
        super(RecoveryPhraseInput, self).__init__(IMAGES_PATH + '/recovery_phrase_input.png')


class RecoverPasswordInput(InputField):
    def __init__(self):
        super(RecoverPasswordInput, self).__init__(IMAGES_PATH + '/recover_password_input.png')


class SignInButton(BaseElement):
    def __init__(self):
        super(SignInButton, self).__init__(IMAGES_PATH + '/sign_in_button.png')


class PasswordInput(InputField):
    def __init__(self):
        super(PasswordInput, self).__init__(IMAGES_PATH + '/password_input.png')


class SignInView(BaseView):
    def __init__(self):
        super(SignInView, self).__init__()
        self.create_account_button = CreateAccountButton()
        self.i_have_account_button = IHaveAccountButton()
        self.other_accounts_button = OtherAccountsButton()
        self.privacy_policy_button = PrivacyPolicyButton()
        self.create_password_input = CreatePasswordInput()
        self.confirm_password_input = ConfirmPasswordInput()
        self.username_input = UserNameInput()
        self.next_button = NextButton()
        self.recovery_phrase_input = RecoveryPhraseInput()
        self.recover_password_input = RecoverPasswordInput()
        self.sign_in_button = SignInButton()
        self.password_input = PasswordInput()

    def create_account(self, password='qwerty', username='test'):
        self.create_account_button.click()
        self.create_password_input.input_value(password)
        self.next_button.click()
        self.confirm_password_input.input_value(password)
        self.next_button.click()
        self.username_input.input_value(username)
        self.next_button.click()
        self.home_button.find_element()
        return HomeView()

    def recover_access(self, passphrase):
        self.i_have_account_button.click()
        self.recovery_phrase_input.send_as_key_event(passphrase)
        self.recover_password_input.send_as_key_event('123456')
        self.sign_in_button.click()
        self.home_button.find_element()
        return HomeView()
