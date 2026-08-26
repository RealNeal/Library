import re
import xml.sax.saxutils as x

def camel_to_snake(name):
    s1 = re.sub(r'(.)([A-Z][a-z]+)', r'\1_\2', name)
    return re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', s1).lower()

fields = []
with open('app/src/main/java/com/rn/library/ui/Strings.kt', encoding='utf-8') as f:
    for line in f:
        m = re.match(r'\s+val (\w+): String', line)
        if m:
            fields.append(m.group(1))

text = open('app/src/main/java/com/rn/library/ui/LocalizedStrings.kt', encoding='utf-8').read()

def parse_block(start_marker, end_marker):
    start = text.index(start_marker)
    end = text.index(end_marker, start)
    block = text[start:end]
    values = {}
    for m in re.finditer(r'(\w+) = "((?:[^"\\]|\\.)*)"', block):
        values[m.group(1)] = m.group(2)
    return values

en = parse_block('val english: Strings = Strings(', '\n    )\n\n    val russian')
ru = parse_block('val russian: Strings = english.copy(', '\n    )\n}')
ru.update(en)

en['importBackupSummary'] = 'Imported: %1$d of %2$d'
ru['importBackupSummary'] = 'Импортировано: %1$d из %2$d'
en['generateUnitsFromCount'] = 'Create %1$d items'
ru['generateUnitsFromCount'] = 'Создать %1$d шт.'

def write_xml(path, values):
    lines = ['<resources>']
    for field in fields:
        key = camel_to_snake(field)
        val = values.get(field, '')
        lines.append('    <string name="' + key + '">' + x.escape(val) + '</string>')
    lines.append('</resources>')
    with open(path, 'w', encoding='utf-8') as out:
        out.write('\n'.join(lines) + '\n')

write_xml('app/src/main/res/values/strings.xml', en)
write_xml('app/src/main/res/values-ru/strings.xml', ru)
print('ok', len(fields))
