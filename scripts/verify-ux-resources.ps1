$ErrorActionPreference = 'Stop'
$uxRoot = Split-Path $PSScriptRoot -Parent
$uxRes = Join-Path $uxRoot 'app/src/main/res'
$uxLayoutDir = Join-Path $uxRes 'layout'
$uxSources = Get-ChildItem (Join-Path $uxRoot 'app/src/main/java') -Recurse -Filter '*.kt'
$uxReferences = [Collections.Generic.HashSet[string]]::new()
foreach ($uxSource in $uxSources) {
    foreach ($uxMatch in [regex]::Matches((Get-Content $uxSource.FullName -Raw -Encoding UTF8), 'R\.layout\.([a-z0-9_]+)')) {
        [void]$uxReferences.Add($uxMatch.Groups[1].Value)
    }
}
$uxErrors = [Collections.Generic.List[string]]::new()
$uxLayouts = @{}
foreach ($uxFile in Get-ChildItem $uxLayoutDir -Filter '*.xml') {
    [xml]$uxXml = Get-Content $uxFile.FullName -Raw -Encoding UTF8
    $uxLayouts[$uxFile.BaseName] = $uxXml
}
do {
    $uxOldCount = $uxReferences.Count
    foreach ($uxName in @($uxReferences)) {
        foreach ($uxMatch in [regex]::Matches($uxLayouts[$uxName].OuterXml, '@layout/([a-z0-9_]+)')) {
            [void]$uxReferences.Add($uxMatch.Groups[1].Value)
        }
    }
} while ($uxReferences.Count -gt $uxOldCount)
foreach ($uxName in $uxReferences) {
    $uxXml = $uxLayouts[$uxName]
    foreach ($uxNode in $uxXml.SelectNodes('//*')) {
        if ($uxNode.GetAttribute('android:fontFamily') -eq 'serif' -or $uxNode.GetAttribute('android:textStyle') -eq 'italic') {
            $uxErrors.Add("${uxName}: legacy serif/italic typography")
        }
        if ($uxNode.Name -eq 'com.google.android.material.card.MaterialCardView' -and
            $uxNode.GetAttribute('app:cardBackgroundColor') -in @('@color/surface_light', '?attr/colorSurface') -and
            $uxNode.GetAttribute('app:strokeWidth') -notin @('', '0dp')) {
            $uxErrors.Add("${uxName}: outlined neutral card")
        }
    }
}
foreach ($uxName in @('bg_card_surface', 'bg_summary_item')) {
    [xml]$uxXml = Get-Content (Join-Path $uxRes "drawable/$uxName.xml") -Raw -Encoding UTF8
    if ($uxXml.shape.solid.GetAttribute('android:color') -ne '@color/surface_light' -or $uxXml.shape.stroke) {
        $uxErrors.Add("${uxName}: expected opaque surface without stroke")
    }
}
if ($uxErrors.Count) { throw ($uxErrors -join "`n") }
Write-Output "PASS: $($uxLayouts.Count) layouts parsed; $($uxReferences.Count) code-referenced/include layouts checked."
Write-Output 'This is a static style regression check, not device, accessibility, or interaction verification.'
