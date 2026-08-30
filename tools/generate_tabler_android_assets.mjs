import fs from 'node:fs';
import path from 'node:path';

const source = process.argv[2];
const output = process.argv[3];
const catalogOutput = process.argv[4];
if (!source || !output || !catalogOutput) throw new Error('source, output and catalogOutput are required');

const xmlEscape = value => value.replaceAll('&', '&amp;').replaceAll('"', '&quot;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
const attr = (tag, name) => new RegExp(`\\s${name}="([^"]*)"`).exec(tag)?.[1] ?? '';
const toPath = (tag, type) => {
  if (type === 'path') return attr(tag, 'd');
  if (type === 'line') return `M ${attr(tag, 'x1')} ${attr(tag, 'y1')} L ${attr(tag, 'x2')} ${attr(tag, 'y2')}`;
  if (type === 'polyline' || type === 'polygon') {
    const points = attr(tag, 'points').trim().split(/[ ,]+/).filter(Boolean);
    let d = '';
    for (let i = 0; i < points.length; i += 2) d += `${i === 0 ? 'M' : 'L'} ${points[i]} ${points[i + 1]} `;
    return type === 'polygon' ? `${d}Z` : d;
  }
  if (type === 'circle') {
    const cx = Number(attr(tag, 'cx')), cy = Number(attr(tag, 'cy')), r = Number(attr(tag, 'r'));
    return `M ${cx - r} ${cy} a ${r} ${r} 0 1 0 ${r * 2} 0 a ${r} ${r} 0 1 0 ${-r * 2} 0`;
  }
  if (type === 'rect') {
    const x = Number(attr(tag, 'x') || 0), y = Number(attr(tag, 'y') || 0);
    const w = Number(attr(tag, 'width')), h = Number(attr(tag, 'height'));
    return `M ${x} ${y} h ${w} v ${h} h ${-w} Z`;
  }
  return '';
};

fs.mkdirSync(output, { recursive: true });
const collectSvgFiles = directory => fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
  const full = path.join(directory, entry.name);
  if (entry.isDirectory()) return collectSvgFiles(full);
  return entry.name.endsWith('.svg') ? [full] : [];
});
const files = collectSvgFiles(source);
let names = [];
for (const file of files) {
  const base = path.basename(file, '.svg').replaceAll('-', '_');
  const text = fs.readFileSync(file, 'utf8');
  const body = [...text.matchAll(/<(path|line|polyline|polygon|circle|rect)\b[^>]*>/g)].map(match => {
    const tag = match[0], type = match[1];
    const d = toPath(tag, type);
    if (!d || attr(tag, 'stroke') === 'none') return '';
    const fill = attr(tag, 'fill');
    // Tabler sets stroke="currentColor" on the SVG root, so path elements
    // inherit it rather than declaring stroke themselves.
    const stroke = attr(tag, 'stroke') || 'currentColor';
    const fillColor = fill && fill !== 'none' ? '#FF17181A' : '@android:color/transparent';
    const strokeColor = stroke && stroke !== 'none' ? '#FF17181A' : '@android:color/transparent';
    return `        <path android:fillColor="${fillColor}" android:strokeColor="${strokeColor}" android:strokeWidth="${attr(tag, 'stroke-width') || '2'}" android:strokeLineCap="${attr(tag, 'stroke-linecap') || 'round'}" android:strokeLineJoin="${attr(tag, 'stroke-linejoin') || 'round'}" android:pathData="${xmlEscape(d)}"/>`;
  }).filter(Boolean).join('\n');
  if (!body) continue;
  fs.writeFileSync(path.join(output, `tabler_${base}.xml`), `<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n${body}\n</vector>\n`);
  names.push(`tabler_${base}`);
}
names = [...new Set(names)].sort();
const groups = [
  ['常用', ['arrow', 'check', 'chevron', 'x', 'plus', 'minus', 'edit', 'trash', 'search', 'calendar', 'settings', 'dots', 'menu', 'home', 'book', 'category', 'wallet']],
  ['财务', ['cash', 'coin', 'coins', 'currency', 'credit-card', 'wallet', 'receipt', 'building-bank', 'pig-money', 'chart', 'chart-pie', 'chart-donut', 'chart-line', 'graph', 'report', 'briefcase']],
  ['出行', ['car', 'bus', 'train', 'plane', 'bike', 'walk', 'map-pin', 'gas-station', 'route', 'taxi', 'motorbike']],
  ['餐饮', ['tools-kitchen', 'chef-hat', 'restaurant', 'coffee', 'pizza', 'bowl', 'mug', 'cake', 'burger', 'apple', 'beer', 'wine']],
  ['居住', ['home', 'bed', 'sofa', 'bath', 'building', 'building-skyscraper', 'lamp', 'door', 'key']],
  ['购物', ['shopping-cart', 'shopping-bag', 'shirt', 'device-mobile', 'device-laptop', 'device-tv', 'gift', 'basket', 'barcode']],
  ['工作', ['briefcase', 'school', 'book', 'clipboard', 'file', 'pencil', 'tool', 'code', 'printer', 'calendar']],
  ['生活', ['heart', 'star', 'camera', 'music', 'gamepad', 'movie', 'paw', 'plant', 'sun', 'moon']],
  ['通讯', ['phone', 'mail', 'message', 'messages', 'send', 'bell', 'world', 'wifi', 'bluetooth']],
  ['设备', ['device-desktop', 'device-tablet', 'device-watch', 'keyboard', 'headphones', 'microphone', 'printer', 'database']],
  ['其他', ['circle', 'help', 'info-circle', 'flag', 'tag', 'bookmark', 'bolt', 'adjustments']]
];
const groupLines = groups.map(([label, keys]) => `        IconGroup("${label}", listOf(${keys.flatMap(key => names.filter(name => name === `tabler_${key}` || name.startsWith(`tabler_${key}_`)).slice(0, 24).map(name => `"${name}"`)).join(', ')}))`).join(',\n');
const legacyAliases = {
  ms_rounded_category: 'tabler_category',
  ms_rounded_shopping_cart: 'tabler_shopping_cart',
  ms_rounded_restaurant: 'tabler_tools_kitchen',
  ms_rounded_lunch_dining: 'tabler_tools_kitchen',
  ms_rounded_home: 'tabler_home',
  ms_rounded_hotel: 'tabler_hotel_service',
  ms_rounded_directions_car: 'tabler_car',
  ms_rounded_flight: 'tabler_plane',
  ms_rounded_checkroom: 'tabler_shirt',
  ms_rounded_devices: 'tabler_devices',
  ms_rounded_work: 'tabler_briefcase',
  ms_rounded_account_balance: 'tabler_building_bank',
  ms_rounded_show_chart: 'tabler_chart_line',
  ms_rounded_pie_chart: 'tabler_chart_donut',
  ms_rounded_savings: 'tabler_pig_money',
  ms_rounded_attach_money: 'tabler_cash',
  ms_rounded_add: 'tabler_plus',
  ms_rounded_visibility: 'tabler_eye',
  ms_rounded_visibility_off: 'tabler_eye_off',
  ms_rounded_photo_camera: 'tabler_camera',
  ms_rounded_book: 'tabler_book',
  ms_rounded_archive: 'tabler_archive'
};
const aliasCases = Object.entries(legacyAliases).map(([oldName, newName]) => `            "${oldName}" -> "${newName}"`).join('\n');
const commonNames = [...new Set([
  'tabler_category', 'tabler_shopping_cart', 'tabler_tools_kitchen', 'tabler_home',
  'tabler_hotel_service', 'tabler_car', 'tabler_plane', 'tabler_shirt', 'tabler_devices',
  'tabler_briefcase', 'tabler_building_bank', 'tabler_chart_line', 'tabler_chart_donut',
  'tabler_pig_money', 'tabler_cash', 'tabler_plus', 'tabler_eye', 'tabler_eye_off',
  'tabler_camera', 'tabler_book', 'tabler_archive', 'tabler_wallet', 'tabler_receipt',
  'tabler_grip_vertical', 'tabler_arrow_fork', 'tabler_chevron_down', 'tabler_search',
  'tabler_calendar', 'tabler_edit', 'tabler_trash', 'tabler_x', 'tabler_check'
].filter(name => names.includes(name)))];
const commonCases = commonNames.map(name => `            "${name}" -> R.drawable.${name}`).join('\n');
const kotlin = `package com.example.cardtally.util\n\nimport android.content.Context\nimport com.example.cardtally.R\n\n/** Local Tabler Icons outline catalog, bundled under the MIT license. */\nobject TablerIconCatalog {\n    data class IconGroup(val title: String, val icons: List<String>)\n    val icons: List<String> = listOf(${names.map(name => `"${name}"`).join(', ')})\n    val groups: List<IconGroup> = listOf(\n${groupLines}\n    )\n    fun resourceId(context: Context, iconName: String?): Int {\n        if (iconName.isNullOrBlank()) return 0\n        return context.resources.getIdentifier(iconName, "drawable", context.packageName)\n    }\n    fun resourceId(iconName: String?): Int {\n        return when (iconName) {\n${names.map(name => `            "${name}" -> R.drawable.${name}`).join('\\n')}\n            else -> 0\n        }\n    }\n}\n`;
const compactKotlin = kotlin.replace(
  /    fun resourceId\(iconName: String\?\): Int \{[\s\S]*?\n    \}\n\}/,
  `    private fun normalize(iconName: String?): String? = when (iconName) {
${aliasCases}
            else -> iconName
        }
    fun resourceId(iconName: String?): Int {
        return when (normalize(iconName)) {
${commonCases}
            else -> R.drawable.tabler_category
        }
    }
    fun resourceId(context: Context, iconName: String?): Int {
        val normalized = normalize(iconName) ?: return 0
        return context.resources.getIdentifier(normalized, "drawable", context.packageName)
    }
}`
).replace(
  /    fun resourceId\(context: Context, iconName: String\?\): Int \{[\s\S]*?\n    \}\n    private fun normalize/,
  '    private fun normalize'
);
fs.writeFileSync(catalogOutput, compactKotlin);
console.log(`Generated ${names.length} Tabler vector drawables`);
