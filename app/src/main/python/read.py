# import pyttsx3
# engine = pyttsx3.init()
from com.chaquo.python import Python
from java import jclass
import json

def text_to_speech(text):
    data = json.loads(text)
    print(data)
    jclass("com.franos.main.BlindAssistantMainActivity").speakText(data['text'])

def text_to_speech_google(text):
    print("myLog ", text, len(text), type(text))
#     data = json.loads(text)
    if len(text) > 0:
        print("myLog python to jave speech")
        jclass("com.franos.main.BlindAssistantMainActivity").speakText(text)
    return "ok"

def start_read_json_data(text):
    print("myLog ", text, len(text), type(text))
#     data = json.loads(text)
    if len(text) > 0:
        print("myLog python to jave speech")
        jclass("com.franos.main.BlindAssistantMainActivity").startReadJson(text)
    return "ok"

# def text_to_speech(text):
#     global engine
#     voices = engine.getProperty('voices')
#     engine.setProperty('voice', voices[1].id)  # 选择不同的语音，0是男性，1是女性
#     engine.say(text)
#     engine.runAndWait()



if __name__ == "__main__":
    text = "Hello, this is a text-to-speech test using pyttsx3."
    text_to_speech(text)