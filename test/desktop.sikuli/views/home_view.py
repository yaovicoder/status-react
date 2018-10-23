import os

from views.base_element import BaseElement, InputField
from views.base_view import BaseView

IMAGES_PATH = os.path.join(os.path.dirname(__file__), 'images/home_view')


class PlusButton(BaseElement):
    def __init__(self):
        super(PlusButton, self).__init__(IMAGES_PATH + '/plus_button.png')


class ContactCodeInput(InputField):
    def __init__(self):
        super(ContactCodeInput, self).__init__(IMAGES_PATH + '/contact_code_input.png')


class HomeView(BaseView):
    def __init__(self):
        super(HomeView, self).__init__()
        self.plus_button = PlusButton()
        self.contact_code_input = ContactCodeInput()
