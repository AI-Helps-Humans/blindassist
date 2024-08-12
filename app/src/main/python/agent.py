import textwrap
import google.generativeai as genai
from google.generativeai.types import HarmCategory, HarmBlockThreshold

from json_repair import repair_json
from IPython.display import display
from IPython.display import Markdown
import json
from bs4 import BeautifulSoup
import re
import threading
from urllib.parse import urlparse, urljoin

from agent_prompt import *
from agent_html_processing import *
from java import jclass

from read import text_to_speech_google, start_read_json_data
from urllib.parse import urlparse
import os

JSON_GENERATION_CONFIG={
        "temperature": 0,
        "top_p": 1,
        "response_mime_type": "application/json"
        }

NORMAL_GENERATION_CONFIG={
        "temperature": 0,
        "top_p": 1,
        "response_mime_type": "application/json"
        }

SAFETY_SETTINGS ={
        HarmCategory.HARM_CATEGORY_HATE_SPEECH: HarmBlockThreshold.BLOCK_ONLY_HIGH,
        HarmCategory.HARM_CATEGORY_HARASSMENT: HarmBlockThreshold.BLOCK_ONLY_HIGH,
        HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT: HarmBlockThreshold.BLOCK_ONLY_HIGH,
        # HarmCategory.HARM_CATEGORY_UNSPECIFIED: HarmBlockThreshold.BLOCK_NONE,
        HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT: HarmBlockThreshold.BLOCK_ONLY_HIGH,
    }

# genai.configure(api_key="")
def to_markdown(text):
  text = text.replace('•', '  *')
  return Markdown(textwrap.indent(text, '> ', predicate=lambda _: True))


def help_check_url(url):
    try:
        result = urlparse(url)
        return all([result.scheme, result.netloc])
    except ValueError:
        return False


def help_find_web_url(genmodel, target):
    send_prompt = SEARCH_WEB_PROMPT.format(target)
    resp1 = genmodel.generate_content([send_prompt],generation_config=JSON_GENERATION_CONFIG,safety_settings=SAFETY_SETTINGS)
    j_answer = json.loads(resp1.text)
    return j_answer

def help_summarize(genmodel, target, html_text):
    print(html_text[:200])
    local_chat = genmodel.start_chat()
    summary_aim = SUMMARY_PROMPT.format(target)
    send_prompt = f"{summary_aim}\n{html_text}"
    resp1 = local_chat.send_message([send_prompt],generation_config=NORMAL_GENERATION_CONFIG,safety_settings=SAFETY_SETTINGS)
    print("ori ans of summarize: ", resp1.text)
    send_prompt = SUMMARY_JSON_PROMPT
    resp2 = local_chat.send_message([send_prompt],generation_config=JSON_GENERATION_CONFIG,safety_settings=SAFETY_SETTINGS)

    j_answer = json.loads(resp2.text)
    print("j_answer of summarize: ",j_answer)
    j_answer.pop("category summaries")
    #todo? cache the summary
    # send_prompt = SUMMARY_READ_CATEGORY_PROMPT
    # resp3 = local_chat.send_message([send_prompt],generation_config=NORMAL_GENERATION_CONFIG,safety_settings=SAFETY_SETTINGS)
    return str(j_answer)

def help_read(genmodel, target, html_text):
    local_chat = genmodel.start_chat()
    read_aim = READ_PROMPT.format(target,target)
    send_prompt = f"{read_aim}\n{html_text}"
    resp1 = local_chat.send_message([send_prompt],generation_config=NORMAL_GENERATION_CONFIG,safety_settings=SAFETY_SETTINGS)
    return resp1.text

def help_query(genmodel, target, html_text):
    '''
    https://ai.google.dev/gemini-api/docs/json-mode?hl=zh-cn&lang=python
    '''
    # related_html_text = extract_exact_text_tag(slim_yahoo_html_content, target)
    # print("related_html_text ",related_html_text)
    local_chat = genmodel.start_chat()
    query_aim = QUERY_PROMPT_1.format(target)
    # print(query_aim)
    send_prompt = f"{query_aim}\n{html_text}"
    resp1 = local_chat.send_message([send_prompt],generation_config=JSON_GENERATION_CONFIG,safety_settings=SAFETY_SETTINGS)
    j_answer_1 = json.loads(resp1.text)

    query_aim = QUERY_PROMPT_2.format(target)
    # print(query_aim)
    send_prompt = f"{query_aim}\n{html_text}"
    resp2 = local_chat.send_message([send_prompt],generation_config=JSON_GENERATION_CONFIG,safety_settings=SAFETY_SETTINGS)
    j_answer_2 = json.loads(resp2.text)
    return j_answer_1,j_answer_2

def help_query_new(genmodel, html_text, user_words):
    # print("html_text ",html_text)
    json_text = html_to_json(html_text)
    json_text = simplify_json_elements(json_text) # cleaning
    # print("json_text ",json_text)
    clickable_objects, fillable_objects = get_clickable_fillable_objects(json_text)
    # print("clickable_objects ",clickable_objects, fillable_objects)
    prompt_clickable = EXTRACT_RELEVANT_CONTENT.format(user_words, clickable_objects)
    prompt_fillable = EXTRACT_RELEVANT_CONTENT.format(user_words, fillable_objects)
    valid_clicable_objs = genmodel.generate_content(prompt_clickable,generation_config=JSON_GENERATION_CONFIG)
    # print(valid_clicable_objs)
    valid_fillable_objs = genmodel.generate_content(prompt_fillable,generation_config=JSON_GENERATION_CONFIG)
    valid_clicable_objs = process_gemini_string(valid_clicable_objs.text)
    valid_fillable_objs = process_gemini_string(valid_fillable_objs.text)
    return valid_clicable_objs, valid_fillable_objs

def help_get_url(genmodel, target, html_text):
    send_prompt = EXTRACT_URL.format(target)
    send_prompt = f"{send_prompt}\n{html_text}"
    resp1 = genmodel.generate_content([send_prompt],generation_config=JSON_GENERATION_CONFIG,safety_settings=SAFETY_SETTINGS)
    j_answer = json.loads(resp1.text)
    return j_answer
    

def help_get_fillable_obj(genmodel, target, html_text):
    send_prompt = EXTRACT_FILLOBJ.format(target)
    send_prompt = f"{send_prompt}\n{html_text}"
    resp1 = genmodel.generate_content([send_prompt],generation_config=JSON_GENERATION_CONFIG,safety_settings=SAFETY_SETTINGS)
    j_answer = json.loads(resp1.text)
    return j_answer

def help_get_usr_intent_target(genmodel, user_words):
    send_prompt = f"{ACTION_PROMPT}\n{user_words}"
    print("help_get_usr_intent_target ???")
    response = genmodel.generate_content([send_prompt],generation_config=JSON_GENERATION_CONFIG,safety_settings=SAFETY_SETTINGS)
    j_answer = json.loads(response.text)
    print("help_get_usr_intent_target end???")
    print("help_get_usr_intent_target j_answer ",j_answer)
    intent = j_answer['intent']
    target = j_answer['target']
    return intent, target   


def help_get_usr_fillin_content(genmodel, user_words,target):
    send_prompt = f"{EXTRACT_FILL_TEXT.format(target)}\n{user_words}"
    response = genmodel.generate_content([send_prompt],generation_config=JSON_GENERATION_CONFIG,safety_settings=SAFETY_SETTINGS)
    jans = json.loads(response.text)
    return jans


'''
Declair global variables here, which will only be excute once when android firstly call this file
'''
genmodel = genai.GenerativeModel("models/gemini-1.5-flash")
state_lock = threading.Lock()
html_lock = threading.Lock()
read_lock = threading.Lock()
state_cache = {"current_action":None,"current_target":None}

ACTION_GOTOWEBSITE_START = 1
ACTION_GOTOWEBSITE_END = 2
ACTION_SUMMARIZE = 3
ACTION_READ = 4
ACTION_QUERY = 5
ACTION_CLICK = 6
ACTION_FILLING = 6
ACTION_READ_JSON_ONE_START = 7
ACTION_READ_JSON_ONE_END = 8
ACTION_SCROLL_DOWN = 9

read_cache = {"order_str": 'dfs', "order":None, "read_tag": 'description', "json_data":None, "tree_data_root":None, "current_node":None}
def update_read_cache(json_data, order='dfs', read_tag='description'):
    lock_acquired = read_lock.acquire(blocking=False)
    if lock_acquired:
        read_cache['json_data'] = json_data
        read_cache['tree_data_root'] = json_to_tree(json_data)
        read_cache['order_str'] = order
        read_cache['order'] = dfs_order(read_cache['tree_data_root'])
        read_cache['read_tag'] = read_tag
        read_cache['current_node'] = read_cache['tree_data_root']

        read_lock.release()
        return True
    else:
        return False
    
class TreeNode:
    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)
        self.children = []

def json_to_tree(json_data):
    def create_node(node_data):
        # 提取除children以外的所有属性
        node = TreeNode(**{k: v for k, v in node_data.items() if k != 'children'})
        children_data = node_data.get('children', [])
        for child_data in children_data:
            child_node = create_node(child_data)
            node.children.append(child_node)
        return node

    return create_node(json_data)

def dfs_order(root):
    """Generate DFS order of nodes"""
    stack = [root]
    order = []
    while stack:
        node = stack.pop()
        order.append(node)
        for child in reversed(node.children):
            stack.append(child)
    return order

def find_next_node(node, order):
    idx = order.index(node)
    next_node = order[idx + 1] if idx < len(order) - 1 else None
    return next_node

def find_last_node(node, order):
    idx = order.index(node)
    prev_node = order[idx - 1] if idx > 0 else None
    return prev_node

def find_left_sibling(node):
    """Find left and right siblings of the node"""
    if not node.parent:
        return None
    siblings = node.parent.children
    idx = siblings.index(node)
    left_sibling = siblings[idx - 1] if idx > 0 else None
    return left_sibling

def find_right_sibling(node):
    """Find left and right siblings of the node"""
    if not node.parent:
        return None
    siblings = node.parent.children
    idx = siblings.index(node)
    right_sibling = siblings[idx + 1] if idx < len(siblings) - 1 else None
    return right_sibling

def get_read_text_and_update_pos_dfs(operation="next"):
    lock_acquired = read_lock.acquire(blocking=False)
    if lock_acquired:
        if read_cache['json_data'] is None:
            return False
        current_node = read_cache['current_node']
        order = read_cache['order']
        read_tag = read_cache['read_tag']
        if operation == "next":
            node = find_next_node(current_node, order)
        elif operation == "last":
            node = find_last_node(current_node, order)
        elif operation == "left":
            node = find_left_sibling(current_node, order)
        elif operation == "right":
            node = find_right_sibling(current_node, order)
        if node is None:
            return False, ""
        read_text = getattr(node, read_tag, "")
        read_cache['current_node'] = node
        read_lock.release()
        return True, read_text
    else:
        return False, ""


html_cache = {"real_url":"","html_text_list":[],"html_to_json_list":[],"scrollheight_list":[],"cur_scroll_idx":0,"idx2summary":{}} 
def try_get_html_text():
    try:
        html_text = html_cache['html_text_list'][html_cache['cur_scroll_idx']]
        return html_text
    except:
        return False

def reduce_html_cur_scroll_idx():
    lock_acquired = html_lock.acquire(blocking=False)
    if lock_acquired:
        if len(html_cache['html_text_list']) > 0:
            if html_cache['html_text_list'][0] is None:
                html_cache['cur_scroll_idx'] = min(1, html_cache['cur_scroll_idx']-1)
            else:
                html_cache['cur_scroll_idx'] = min(0, html_cache['cur_scroll_idx']-1)
        html_lock.release()
        

def update_html_cache(real_url, html_text, last_scrollheight, cur_scrollheight):
    lock_acquired = html_lock.acquire(blocking=False)
    if lock_acquired:
        return_str = ""
        if real_url != html_cache['real_url']:
            # reach a new website
            html_text = BeautifulSoup(html_text, "html.parser")
            html_text = clean_html(html_text)
            html_cache['real_url'] = real_url
            html_cache['html_text_list'] = [None,html_text]
            html_cache['idx2summary'] = {}
            if cur_scrollheight > last_scrollheight:
                # this website is scrollable
                # in this case, the first scrollheight is actually the one after one scrolling
                html_cache['html_text_list'] = [None,html_text]
                html_cache['scrollheight_list'] = [last_scrollheight, cur_scrollheight]
                html_cache['cur_scroll_idx'] = 1
                return_str = "first in"
            else:
                # in this case, the website is not scrollable
                # so we can identify whether a website is scollable according to the length of the html_cache['scrollheight']
                # but we can't know whether there is more content to be scrolling
                assert cur_scrollheight == last_scrollheight, "error scrollheight, why?"
                html_cache['html_text_list'] = [html_text]
                html_cache['scrollheight_list'] = [last_scrollheight]
                html_cache['cur_scroll_idx'] = 0
                return_str = "first in"
        else:
            # try to fetch new content on the existing website
            # we don't know whether the website is still scrollable before this callback
            if cur_scrollheight == last_scrollheight:
                # in this case the website is on the bottom, no need to update
                return_str = "on the bottom"
            else:
                html_text = clean_html(html_text)
                html_text = process_html_scroll_content(html_cache['html_text_list'][-1],html_text)
                html_cache['html_text_list'].append(html_text)
                assert html_cache[-1] == last_scrollheight, "error update scrollheight, why?"
                html_cache['scrollheight_list'].append(cur_scrollheight)
                html_cache['cur_scroll_idx'] += 1
                return_str = "scoll down"
        
        html_lock.release()
        return return_str
    else:
        return "update failed"

def check_can_do_action_and_update(to_do_action, target):
    # currently the agent can only do one action at the same time
    lock_acquired = state_lock.acquire(blocking=False)
    if lock_acquired:
        if state_cache['current_action'] is not None:
            if to_do_action ==  ACTION_GOTOWEBSITE_END and state_cache['current_action'] == ACTION_GOTOWEBSITE_START:
                state_cache['current_action'] = to_do_action
                state_cache['current_target'] = target
                state_lock.release()
                return True
            elif to_do_action == ACTION_READ_JSON_ONE_END and state_cache['current_action'] == ACTION_READ_JSON_ONE_START:
                state_cache['current_action'] = to_do_action
                state_cache['current_target'] = target
                state_lock.release()
                return True
            else:
                state_lock.release()
                return False
        else:
            state_cache['current_action'] = to_do_action
            state_cache['current_target'] = target
            state_lock.release()
            return True
    else:
        return False

def end_action_and_update(end_action):
    state_cache['current_action'] = None
    state_cache['current_target'] = None 
    

def do_action_gotowebsite_start(genmodel,target):
    if check_can_do_action_and_update(ACTION_GOTOWEBSITE_START, target):
        jans = help_find_web_url(genmodel,target)
        '''
        todo: check whether the url is true, legal, or the one that user indeed want
        if the answer is not correct, we can ask the user to read the url to the agent
        or list the candidates and ask the user to choose
        '''

        print("find_web_url jans ",jans)
        # the left steps will be done after jave's callback
        url = jans.get('url',"")
        check_url = help_check_url(url)
        if check_url != False:
            jclass("com.franos.main.BlindAssistantMainActivity").loadurl(jans['url'])
            end_action_and_update(ACTION_GOTOWEBSITE_START)
        else:
            print(f"ERROR url: {url}")
    else:
        # todo: if the agent can not do something, should tell the reason to the user
        pass

def do_action_gotowebsite_end(html_text):
    if check_can_do_action_and_update(ACTION_GOTOWEBSITE_END, "webpage"):
        ans = help_summarize(genmodel, "webpage", html_text)
        print("do_action_gotowebsite_end ",ans)
        text_to_speech_google(ans)
        end_action_and_update(ACTION_GOTOWEBSITE_END)

def do_action_scroll_down():
    if check_can_do_action_and_update(ACTION_SCROLL_DOWN, "webpage"):
        ans = help_summarize(genmodel, "webpage", html_text)
        print("do_action_gotowebsite_end ",ans)
        text_to_speech_google(ans)
        end_action_and_update(ACTION_SCROLL_DOWN)

def read(j_answer):
  if 'children' not in j_answer or len(j_answer['children']) == 0:
    # This is the leaf of a tree
    print(f"{j_answer['summary']} is about {j_answer['content']}")
    # text_to_speech_google(j_answer['content'])
    return False. j_answer['content']
  else:
    # This is an internal node
    return True, j_answer

    # print(f"{j_answer['summary']} includes the following sections which you can further investigate: ")
    # sum = []
    # for item in j_answer['children']:
    #   print(item['summary'])
    #   sum.append(item['summary'])
    # return sum
  
def find_by_summary(nested_dict, target_summary):
    # Check if the current dictionary has the target summary
    if nested_dict.get('summary').lower() == target_summary.lower():
        return nested_dict

    # If 'children' exists, iterate through each child
    if 'children' in nested_dict:
        for child in nested_dict['children']:
            result = find_by_summary(child, target_summary)
            if result:
                return result
    return None

def page_to_json(genmodel, html_text):
  soup = BeautifulSoup(html_text,'html.parser')
  slim_html_content = clean_html(soup)

  local_chat = genmodel.start_chat()
  summary_aim = READ_PROMPT
  send_prompt = f"{summary_aim}\n{slim_html_content}"
  resp1 = local_chat.send_message([send_prompt],generation_config=JSON_GENERATION_CONFIG)
  good_json_string = repair_json(resp1.text)
#   good_json_string = resp1.text
  j_answer = json.loads(good_json_string)
  return j_answer

def summarize_target(j_answer, item=None):
  if item is None or find_by_summary(j_answer, item) is None:
    return read(j_answer)
  else:
    target_dict = find_by_summary(j_answer, item)
    return read(target_dict)

def get_summarize_jans():
    cur_scroll_idx = html_cache['cur_scroll_idx']
    idx2summary = html_cache['idx2summary']
    return idx2summary.get(cur_scroll_idx, None)

def set_summarize_jans(jans):
    cur_scroll_idx = html_cache['cur_scroll_idx']
    idx2summary = html_cache['idx2summary']
    idx2summary[cur_scroll_idx] = jans
  
def do_action_summarize_read(genmodel,target):
    if check_can_do_action_and_update(ACTION_SUMMARIZE, target):
        html_text = try_get_html_text()
        if html_text == False:
            print("get html_text failed!")
            end_action_and_update(ACTION_SUMMARIZE)
            return
        j_answer = get_summarize_jans()
        if j_answer is None:
            j_answer = page_to_json(genmodel, html_text)
            set_summarize_jans(j_answer)
        # summarize_target(j_answer, target)

        if target is None or find_by_summary(j_answer, target) is None:
            useJsonRead, content =  read(j_answer)
        else:
            target_dict = find_by_summary(j_answer, target)
            useJsonRead, content = read(target_dict)
        end_action_and_update(ACTION_SUMMARIZE)
        if useJsonRead == False:
            text_to_speech_google(content)
        else:
            success = update_read_cache(j_answer, order='dfs', read_tag='summary')
            if success == False:
                text_to_speech_google("Summarize failed, please summarize it again")
            else:
                do_action_read_json_start()

def do_action_summarize(genmodel,target):
    if check_can_do_action_and_update(ACTION_SUMMARIZE, target):
        html_text = try_get_html_text()
        if html_text == False:
            print("get html_text failed!")
            end_action_and_update(ACTION_SUMMARIZE)
            return
        ans = help_summarize(genmodel, target, html_text)
        text_to_speech_google(ans)
        end_action_and_update(ACTION_SUMMARIZE)

def do_action_read(genmodel,target):
    if check_can_do_action_and_update(ACTION_READ, target):
        html_text = try_get_html_text()
        if html_text == False:
            print("get html_text failed!")
            end_action_and_update(ACTION_READ)
            return
        ans = help_read(genmodel, target, html_text)
        text_to_speech_google(str(ans))
        end_action_and_update(ACTION_READ)

def do_action_query(genmodel,target, user_words):
    if check_can_do_action_and_update(ACTION_QUERY, target):
        html_text = try_get_html_text()
        if html_text == False:
            print("get html_text failed!")
            end_action_and_update(ACTION_QUERY)
            return
        jans1,jans2 = help_query_new(genmodel,html_text,user_words)
        merged_ans = jans1 + jans2
        success = update_read_cache(merged_ans, order='dfs', read_tag='description')
        if success == False:
            text_to_speech_google("Query failed, please query it again")
        end_action_and_update(ACTION_QUERY)
        if success == True:
            do_action_read_json_start()
            
def do_action_read_json_start():
    if check_can_do_action_and_update(ACTION_READ_JSON_ONE_START, None):
        success, text = get_read_text_and_update_pos_dfs(operation="next")
        if success == True:
            start_read_json_data(text)
        else: 
            text_to_speech_google("Query failed, please query it again")

def do_action_read_json_end(direction="Down"):
    if check_can_do_action_and_update(ACTION_READ_JSON_ONE_END, None):
        end_action_and_update(ACTION_READ_JSON_ONE_END)
        if direction == 'Up':
            operation = 'last'
        elif direction == 'Down':
            operation = 'next'
        elif direction == 'Left':
            operation = 'left'
        elif direction == 'right':
            operation = 'right'
        success, text = get_read_text_and_update_pos_dfs(operation)
        if success == True:
            if check_can_do_action_and_update(ACTION_READ_JSON_ONE_START, None):
                start_read_json_data(text)
        else: 
            pass

def do_action_click(genmodel,target):
    if check_can_do_action_and_update(ACTION_CLICK, target):
        html_text = try_get_html_text()
        if html_text == False:
            print("get html_text failed!")
            end_action_and_update(ACTION_CLICK)
            return
        jans = help_get_url(genmodel,target, html_text)
        url = jans.get('url',"")
        
        parsed_url = urlparse(url)
        final_url = ""
        if bool(parsed_url.netloc):
            final_url = url
        else:
            cur_url = html_cache['real_url']
            final_url = urljoin(cur_url, url)
        print("do_action_click final_url ",final_url)
        jclass("com.franos.main.BlindAssistantMainActivity").loadurl(final_url)
        end_action_and_update(ACTION_CLICK)

def do_action_fill_in(genmodel, target, user_words):
    if check_can_do_action_and_update(ACTION_FILLING, target):
        html_text = try_get_html_text()
        if html_text == False:
            print("get html_text failed!")
            end_action_and_update(ACTION_FILLING)
            return
        jans = help_get_usr_fillin_content(genmodel,user_words,target)
        
        jsScript = f"""
            (function() {{
                document.getElementById('{jans['field name']}').value = '{jans['content']}';
            }})();
        """

        jclass("com.franos.main.BlindAssistantMainActivity").fillins(jsScript)
        end_action_and_update(ACTION_FILLING)


def finish_get_html(kwargs):
#     print(type(kwargs.keySet()), dir(kwargs.keySet()))
#     print(dir(kwargs))
#     print(kwargs.get("url"))
#     kwargs = dict(kwargs)
#     kwargs = dict(kwargs)
    html_text = kwargs.get('html')
    real_url = kwargs.get('url')
    last_scrollheight = int( kwargs.get('last_scrollheight'))
    cur_scrollheight = int( kwargs.get('cur_scrollheight'))
    html_update_ret = update_html_cache(real_url, html_text, last_scrollheight, cur_scrollheight)
    print("finish_get_html ",real_url,last_scrollheight,cur_scrollheight)
    print("finish_get_html ",html_update_ret)
    if html_update_ret == "first in":
        do_action_gotowebsite_end(html_cache['html_text_list'][html_cache['cur_scroll_idx']])
    elif html_update_ret == "on the bottom":
        # todo: tell the user it's on the bottom
        text_to_speech_google("The webpage has reached the bottom.")
        pass
    elif html_update_ret == "scoll down":
        # todo: tell the user the new content
        do_action_summarize(genmodel,"webpage")
        pass
    elif html_update_ret == "update failed":
        # todo: tell the app that update failed
        pass


testIdx = 0
def agent(user_words):
    # for user_input in user_input_list:

    # user_input = input("请输入一些内容：")
#         if user_words == 'quit':
#             break
    print(user_words)

    intent, target = help_get_usr_intent_target(genmodel, user_words)
    print(f"intent {intent}, target {target}")
    # "click", "fill in", "read", "summarize", and "go to the website"
    if intent.lower() == 'go to the website':
        do_action_gotowebsite_start(genmodel,target)
    # elif intent.lower() == 'summarize':
    #     do_action_summarize(genmodel,target)
    # elif intent.lower() == 'read':
    #     do_action_read(genmodel, target)
    elif intent.lower() == 'summarize' or intent.lower() == 'read':
        do_action_summarize_read(genmodel,target)
    elif intent.lower() == 'query':
        do_action_query(genmodel, target, user_words)
    elif intent.lower() == 'click':
        do_action_click(genmodel,target)
    elif intent.lower() == 'fill in':
        do_action_fill_in(genmodel, target, user_words)
#
#
# def main(args):
#     # 主函数逻辑，处理命令行参数
#     print(f"Arguments: {args}")
#

if __name__ == "__main__":

    from selenium import webdriver
    import time

    # driver_path = '/path/to/chromedriver'  # 替换为你自己的路径

    # 初始化 WebDriver
    driver = webdriver.Edge(executable_path="app/src/main/python/msedgedriver.exe")

    # 打开目标网页
    url = 'https://www.example.com'
    driver.get(url)

    # 模拟滚动到底部触发新内容加载
    scroll_pause_time = 2  # 每次滚动后暂停的时间（秒）

    # 获取初始页面高度
    last_height = driver.execute_script("return document.body.scrollHeight")

    while True:
        # 滚动到底部
        driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
        
        # 等待页面加载新内容
        time.sleep(scroll_pause_time)
        
        # 再次获取页面高度
        new_height = driver.execute_script("return document.body.scrollHeight")
        
        # 检查是否加载到最后，页面高度没有变化
        if new_height == last_height:
            break
        
        last_height = new_height

    # 获取滚动到底部后的页面HTML
    html = driver.page_source
    print(html)

    # 关闭 WebDriver
    driver.quit()

    with open("app/src/main/python/yahoofinance_iphone15pm_edge/html/yahoofinance_1.html") as f:
        yahoo_html_content = f.read()

    html_text = BeautifulSoup(yahoo_html_content, "html.parser")
    html_text = clean_html(html_text)
    target = "news list"
    user_words = "query the information about the Aussie's secret"
    str_jans1,str_jans2 = help_query_new(genmodel,html_text,user_words)
    print(json.loads(str_jans1))
    print(json.loads(str_jans2))
