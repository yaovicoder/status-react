#ifndef DESKTOPLINKING_H
#define DESKTOPLINKING_H

#include "moduleinterface.h"

#include <QVariantMap>
#include <QUrl>
#include <QFileOpenEvent>

class DesktopLinkingPrivate;
class DesktopLinking : public QObject, public ModuleInterface {
    Q_OBJECT
    Q_INTERFACES(ModuleInterface)

    Q_DECLARE_PRIVATE(DesktopLinking)

public:
    Q_INVOKABLE DesktopLinking(QObject* parent = 0);
    ~DesktopLinking();

    void setBridge(Bridge* bridge) override;

    QString moduleName() override;
    QList<ModuleMethod*> methodsToExport() override;
    QVariantMap constantsToExport() override;

    Q_INVOKABLE void setCallback(double callbackId);

signals:
    void urlOpened(QString path);
    void fileOpened(QString path);

public slots:
    void handleURL(const QString url);

private:
    QScopedPointer<DesktopLinkingPrivate> d_ptr;
    bool eventFilter(QObject* obj, QEvent* event)
    {
        if (event->type() == QEvent::FileOpen)
        {
            QFileOpenEvent* fileEvent = static_cast<QFileOpenEvent*>(event);
            if (!fileEvent->url().isEmpty())
            {
                auto m_lastUrl = fileEvent->url().toString();
                emit urlOpened(m_lastUrl);
            }
            else if (!fileEvent->file().isEmpty())
            {
                emit fileOpened(fileEvent->file());
            }

            return false;
        }
        else
        {
            // standard event processing
            return QObject::eventFilter(obj, event);
        }
    }
};

#endif // DESKTOPLINKING_H
