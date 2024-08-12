
import json
from bs4 import BeautifulSoup
import re
import time 


def extract_exact_text_tag(html_text, search_text):
    soup = BeautifulSoup(html_text, 'html.parser')
    
    # Find all tags that contain the search text
    tags_with_text = [tag for tag in soup.find_all() if search_text in tag.get_text()]
    # print(tags_with_text)
    # Iterate over the found tags to find the exact match
    for tag in tags_with_text:
        # Check each direct descendant of the tag for the exact text match
        for descendant in tag.descendants:
            if descendant.string and search_text in descendant.string:
                return str(descendant)
    
    return None

def clean_html(soup):
    ''' Y.D '''
    [script.extract() for script in soup.findAll('script')]
    [style.extract() for style in soup.findAll('style')]
    [path.extract() for path in soup.findAll('path')]
    # todo  保留head
    # [head.extract() for head in soup.findAll('head')]

    attr_to_del = ['class', 'height', 'width', 'viewbox', 'style', 'data-']

    for tag in soup.find_all(True):  # True finds all tags
      found_keywords = []
      for key in tag.attrs.keys():
        for attr in attr_to_del:
          if attr in key:
            found_keywords.append(key)
      for key in found_keywords:
        del tag[key]

    return str(soup)

def process_html_scroll_content(previous_html, current_html):
    """
    找到current_soup中相对于previous_soup的新增元素
    """
    previous_soup = BeautifulSoup(previous_html, 'html.parser')
    current_soup = BeautifulSoup(current_html, 'html.parser')
    previous_elements = set(previous_soup.find_all(True))
    current_elements = set(current_soup.find_all(True))

    to_remove_candidates = []
    new_elements = []
    for elem in current_elements:
        if elem in previous_elements:
            to_remove_candidates.append(elem)
        else:
            new_elements.append(elem)

    for elem in new_elements:
        if not any(child in new_elements for child in elem.find_all()):
            for ancestor in elem.parents:
                ancestor['blind-html-update-cannot-remove'] = 'true'
    
    # print(lowest_level_new_elements)
    # return lowest_level_new_elements
    print("to_remove_candidates ",to_remove_candidates)
    final_remove_list = []
    for elem in to_remove_candidates:
        if elem.has_attr('blind-html-update-cannot-remove'):
            continue
        else:
            all_siblings = elem.find_next_siblings()
            for sibling in all_siblings:
                if not sibling.has_attr('blind-html-update-cannot-remove'):
                    final_remove_list.append(elem)
                    break

    for to_remove in final_remove_list:
        to_remove.decompose()
    
    return str(current_html)

def get_widget_names(json_data):
    ''' Tina '''

    # Parse JSON data
    widgets = json.loads(json_data)

    # Initialize a list to store names
    names = []

    # Iterate through widgets and extract names
    for widget in widgets:
        name = widget.get('name', '')  # Get the 'name' key from each widget dictionary
        if name:
            names.append(name)

    return names

def extract_fields(input_string):
    ''' Tina '''
    # Split the input string by lines
    lines = input_string.splitlines()

    # Initialize an empty list to store topics
    topics = []

    # Iterate through each line starting from the second line
    for line in lines[1:]:
        # Split each line by the first occurrence of a digit followed by a dot
        parts = line.split('. ', 1)
        if len(parts) > 1:
            topics.append(parts[1].strip())

    return topics

def filter_widgets_by_name(json_data, name_pool):
    ''' Tina '''
    # Parse JSON data
    widgets = json.loads(json_data)

    # Initialize a list to store filtered widgets
    filtered_widgets = []

    # Iterate through widgets and filter by name pool
    for widget in widgets:
        widget_name = widget.get('name', '')
        if widget_name in name_pool:
            filtered_widgets.append(widget)

    return filtered_widgets

def simplify_element(element):
    ''' Tina '''
    if not isinstance(element, dict):
        return element

    # Recursively simplify the children first
    simplified_children = [simplify_element(child) for child in element.get('children', [])]

    # If the element has only one child and they have the same tag and text, simplify
    while len(simplified_children) == 1:
        child = simplified_children[0]
        if child['tag'] == element['tag'] and child.get('text', '') == element.get('text', ''):
            simplified_children = child.get('children', [])
        else:
            break

    # Update the children with the simplified version
    element['children'] = simplified_children
    return element

def clean_json(input_data):
    ''' Tina '''
    # Check if input_data is a string and parse it as JSON if necessary
    if isinstance(input_data, str):
        data = json.loads(input_data)
    else:
        data = input_data

    # Simplify each element in the list
    simplified_data = [simplify_element(element) for element in data]
    return simplified_data
    # return json.dumps(simplified_data, indent=2) if isinstance(input_data, str) else simplified_data


'''QUERY HELPER FUNCTIONS'''
from bs4 import BeautifulSoup
def html_to_json(html_content):
    ''' Tina '''
    def element_to_dict(element):
        element_dict = {
            'tag': element.name,
            'text': element.get_text(strip=True),
            'children': []
        }

        # Preserve href for 'a' tags
        if element.name == 'a':
            element_dict['href'] = element.get('href')

        # Preserve fillable attributes for input and textarea
        if element.name == 'input':
            element_dict['type'] = element.get('type')
            element_dict['name'] = element.get('name')
            element_dict['placeholder'] = element.get('placeholder')
            element_dict['value'] = element.get('value')
        if element.name == 'textarea':
            element_dict['name'] = element.get('name')
            element_dict['placeholder'] = element.get('placeholder')

        for child in element.children:
            if child.name is not None:  # Exclude NavigableString instances
                element_dict['children'].append(element_to_dict(child))

        return element_dict
    soup = BeautifulSoup(html_content, 'html.parser')
    result = [element_to_dict(child) for child in soup.body.children if child.name is not None]
    return result

def simplify_json_elements(element):
    if not isinstance(element, dict):
        return element

    stack = [element]

    while stack:
        current = stack.pop()
        if not isinstance(current, dict) or 'children' not in current:
            continue

        simplified_children = []
        for child in current['children']:
            stack.append(child)
            simplified_grandchildren = child.get('children', [])
            # If child has the same tag and text as the current, skip the child and add its children
            if child.get('tag') == current.get('tag') and child.get('text', '') == current.get('text', ''):
                simplified_children.extend(simplified_grandchildren)
            else:
                simplified_children.append(child)

        current['children'] = simplified_children

    return element


def get_clickable_fillable_objects(json_text):
    clickable_objects = get_clickable_objects(json_text)
    fillable_objects = get_fillable_objects(json_text)
    return clickable_objects,fillable_objects

def get_clickable_objects(json_text):
    clickable_objects = []
    collect_clickable_objects(json_text, clickable_objects)
    return clickable_objects

def get_fillable_objects(json_text): # Previous get_fillable_obj(genmodel, target, html_text)
    fillable_objects = []
    collect_fillable_objects(json_text, fillable_objects)
    return fillable_objects

def collect_clickable_objects(item, clickable_objects):
    if isinstance(item, dict):
        # Check if the item is clickable, assuming "href" attribute indicates clickability
        if 'href' in item:
            clickable_objects.append(item)
        # Recursively traverse children if they exist
        if 'children' in item and isinstance(item['children'], list):
            for child in item['children']:
                collect_clickable_objects(child, clickable_objects)
    elif isinstance(item, list):
        for sub_item in item:
            collect_clickable_objects(sub_item, clickable_objects)

def collect_fillable_objects(item, fillable_objects):
    if isinstance(item, dict):
        # Check if the item is fillable
        if item.get('tag') == 'input' and item.get('type') in {'text', 'search', 'password', 'email', 'url', 'number'}:
            fillable_objects.append(item)
        elif item.get('tag') == 'textarea':
            fillable_objects.append(item)
        # Recursively traverse children if they exist
        if 'children' in item and isinstance(item['children'], list):
            for child in item['children']:
                collect_fillable_objects(child, fillable_objects)
    elif isinstance(item, list):
        for sub_item in item:
            collect_fillable_objects(sub_item, fillable_objects)

def generate_description(component, layer=0):
    indent = '  ' * layer
    desc = f"Layer {layer + 1}: This is a '{component['tag']}' element."
    if 'text' in component and component['text']:
        desc += f" It contains the text: '{component['text']}'."
    if 'href' in component:
        desc += f" It links to: '{component['href']}'."
    if 'children' in component and component['children']:
        desc += f" It has {len(component['children'])} child elements."
    return desc

def add_description(component, layer=0):
    component['description'] = generate_description(component, layer)
    if 'children' in component:
        for child in component['children']:
            add_description(child, layer + 1)
    return component

def process_gemini_string(gemini_ans):
    # print("gemini_ans ",gemini_ans)
    json_data = json.loads(gemini_ans)
    annotated_json = [add_description(comp) for comp in json_data]
    # return json.dumps(annotated_json, indent=4)
    return annotated_json

