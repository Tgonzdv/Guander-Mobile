Add-Type -AssemblyName System.IO.Compression.FileSystem
$docxPath = "C:\Users\tomas\AndroidStudioProjects\Guander\Manual guia\Manual de usuario.docx"
$zip = [System.IO.Compression.ZipFile]::OpenRead($docxPath)
$entry = $zip.Entries | Where-Object { $_.FullName -eq "word/document.xml" }
$stream = $entry.Open()
$reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
$xmlContent = $reader.ReadToEnd()
$reader.Close()
$zip.Dispose()
[xml]$doc = $xmlContent
$nsm = New-Object System.Xml.XmlNamespaceManager($doc.NameTable)
$nsm.AddNamespace("w", "http://schemas.openxmlformats.org/wordprocessingml/2006/main")
$paragraphs = $doc.SelectNodes("//w:p", $nsm)
$output = @()
foreach ($p in $paragraphs) {
    $runs = $p.SelectNodes(".//w:t", $nsm)
    $line = ($runs | ForEach-Object { $_.InnerText }) -join ""
    $output += $line
}
$output -join "`n" | Out-File "C:\Users\tomas\AndroidStudioProjects\Guander\manual_extracted.txt" -Encoding UTF8
Write-Output "Done. Lines: $($output.Count)"
